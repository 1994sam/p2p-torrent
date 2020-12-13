package org.networks.java.service;

import org.networks.java.helper.Constants;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.HandshakeMessage;
import org.networks.java.model.Message;
import org.networks.java.model.PeerInfo;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

import static org.networks.java.helper.Constants.MessageType.*;

public class Client implements Runnable {

	@Override
	public void run() {
		try {
			do {
				processHandShake();
			} while (!connectionEstablished);
			sendBitFieldMsg();

			new Thread(() -> {
				try {
					pushMsgToQueue();
				} catch (InterruptedException | IOException ex) {
				}
			}).start();

			while (!shutdown)
				processMessage();

		} catch (EOFException e) {
			msgStreamQueue.add(new Message(null, null));
		} catch (Exception ex) {
		}
	}

	private void processMessage() throws IOException {
		int messageLength = msgStream.getInputStream().readInt();
		Constants.MessageType messageType = getMessageValue(msgStream.getInputStream().readByte());

		switch (messageType) {
			case CHOKE:
				readChokeMsg();
				break;
			case UNCHOKE:
				readUnchokeMsg();
				break;
			case INTERESTED:
				readInterestedMsg();
				break;
			case NOT_INTERESTED:
				readNotInterested();
				break;
			case HAVE:
				readHaveMsg();
				break;
			case BITFIELD:
				readBitFieldMsg(messageLength);
				break;
			case REQUEST:
				readRequestMsg();
				break;
			case PIECE:
				readPieceMsg(messageLength);
				break;
		}
	}

	private void processHandShake() throws IOException {
		sendHandshakeMsg();
		while (!connectionEstablished)
			receiveHandshakeMsg();

		peer.addClient(this);
	}

	private void pushMsgToQueue() throws InterruptedException, IOException {
		while (true) {
			Message msg = msgStreamQueue.take();
			if (msg.getMessageType() == null)
				return;
			sendMsg(msg);
		}
	}

	private void readChokeMsg() {
		P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] is choked by [" + neighborPeerInfo.getPeerId() + "].");
		lastDownloadRateLock.writeLock().lock();
		lastDownloadRate = (float) downloadedPiecesSinceUnchoked / Duration.between(Instant.now(), lastUnchokedByNeighborAt).getSeconds();
		downloadedPiecesSinceUnchoked = 0;
		lastDownloadRateLock.writeLock().unlock();
	}

	private void readUnchokeMsg() {
		P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.getPeerInfo().getPeerId() + " is unchoked by [" + neighborPeerInfo.getPeerId() + "].");
		lastUnchokedByNeighborAt = Instant.now();
		requestPiece();
	}

	private void readInterestedMsg() {
		P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] received the `interested` message from [" + neighborPeerInfo.getPeerId() + "].");
		peer.addPeersInterestedInMe(neighborPeerInfo.getPeerId());
	}

	private void readNotInterested() {
		P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] received a `not interested` message from [" + neighborPeerInfo.getPeerId() + "].");
		peer.removeFromPeersInterestedInMe(neighborPeerInfo.getPeerId());
	}

	private void readHaveMsg() throws IOException {
		P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] received the `have` message from [" + neighborPeerInfo.getPeerId() + "].");
		Integer pieceIndex = msgStream.read4ByteIntData();
		peer.updateNeighborPieceIndex(neighborPeerInfo.getPeerId(), pieceIndex);
		if (!peer.hasPiece(pieceIndex)) {
			Message msg = new Message(INTERESTED, null);
			msgStreamQueue.add(msg);
		} else {
			Message msg = new Message(NOT_INTERESTED, null);
			msgStreamQueue.add(msg);
		}
	}

	private void readBitFieldMsg(int messageLength) throws IOException {
		byte[] bitFieldByte = new byte[messageLength];
		msgStream.getInputStream().readFully(bitFieldByte);
		peer.setNeighborBitField(neighborPeerInfo.getPeerId(), bitFieldByte);
		if (peer.getPieceRequestIndex(neighborPeerInfo.getPeerId()) != -1) {
			Message msg = new Message(INTERESTED, null);
			msgStreamQueue.add(msg);
		} else {
			Message msg = new Message(NOT_INTERESTED, null);
			msgStreamQueue.add(msg);
		}
	}

	private void readRequestMsg() throws IOException {
		Integer pieceIndex = msgStream.read4ByteIntData();
		if (isChoked)
			return;
		byte[] piece = peer.getPiece(pieceIndex);
		if (piece != null) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4 + piece.length);
			byteBuffer.putInt(pieceIndex);
			byteBuffer.put(piece);
			Message msg = new Message(PIECE, byteBuffer.array());
			msgStreamQueue.add(msg);
		}
	}

	private void readPieceMsg(int messageLength) throws IOException {
		Integer pieceIndex = msgStream.read4ByteIntData();
		byte[] piece = new byte[messageLength - 4];
		msgStream.getInputStream().readFully(piece);
		boolean pieceAdded = peer.addPiece(pieceIndex, piece);
		downloadedPiecesSinceUnchoked++;
		if (pieceAdded) {
			P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] has downloaded the piece [" + pieceIndex + "] from [" + neighborPeerInfo.getPeerId() + "]. "
				+ "Now the number of pieces it has is [" + peer.getFileDownloader().getFilePieces().size() + "].");
		}
		requestPiece();
	}

	private void receiveHandshakeMsg() throws IOException {
		String receivedMsg = msgStream.getInputStream().readUTF();
		String neighborPeerId = receivedMsg.substring(20, 24); //TODO change
		String sentMsg = new HandshakeMessage(neighborPeerId).toString();

		if (sentMsg.equalsIgnoreCase(receivedMsg)) {
			connectionEstablished = true;
			P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] makes a connection to Peer [" + neighborPeerInfo.getPeerId() + "].");
		}
	}

	private void sendMsg(Message msg) throws IOException {
		int msgLen = msg.getMessagePacket() != null ? msg.getMessagePacket().length : 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(msgLen + 5);
		byteBuffer.putInt(msgLen);
		byteBuffer.put((byte) msg.getMessageType().ordinal());

		if (msg.getMessagePacket() != null)
			byteBuffer.put(msg.getMessagePacket());
		msgStream.getOutputStream().write(byteBuffer.array());
	}

	private void sendHandshakeMsg() throws IOException {
		HandshakeMessage handshakeMessage = new HandshakeMessage(peer.getPeerInfo().getPeerId());
		msgStream.getOutputStream().writeUTF(handshakeMessage.toString());
	}

	public void sendBitFieldMsg() throws IOException {
		if (peer.hasOnePiece()) {
			Message msg = new Message(BITFIELD, peer.getBitField());
			sendMsg(msg);
		}
	}

	public void sendHaveMsg(int pieceIndex) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(pieceIndex);
		Message msg = new Message(HAVE, byteBuffer.array());
		msgStreamQueue.add(msg);
	}

	public void chokeNeighbor() {
		isChoked = true;
		Message msg = new Message(CHOKE, null);
		msgStreamQueue.add(msg);
	}

	public void unchokeNeighbor() {
		isChoked = false;
		Message msg = new Message(UNCHOKE, null);
		msgStreamQueue.add(msg);
	}

	private void requestPiece() {
		int pieceIndex = peer.getPieceRequestIndex(neighborPeerInfo.getPeerId());
		if (pieceIndex == -1)
			return;
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(pieceIndex);
		Message msg = new Message(REQUEST, byteBuffer.array());
		msgStreamQueue.add(msg);
	}

	public float getDownloadRate() {
		lastDownloadRateLock.readLock().lock();
		try {
			return lastDownloadRate;
		} finally {
			lastDownloadRateLock.readLock().unlock();
		}
	}

	public void shutdown() {
		try {
			shutdown = true;
			while (!msgStreamQueue.isEmpty()) ;
			msgStream.getOutputStream().flush();
			if (socket != null) {
				msgStream.getInputStream().close();
				msgStream.getOutputStream().close();
				socket.close();
			}
		} catch (IOException e) {
		}
	}

	private final ReadWriteLock lastDownloadRateLock = new ReentrantReadWriteLock();

	private boolean connectionEstablished;
	private boolean isChoked;
	private boolean shutdown;
	private int downloadedPiecesSinceUnchoked;
	private float lastDownloadRate;

	private Instant lastUnchokedByNeighborAt;
	private MessageStream msgStream;

	private Peer peer;
	private PeerInfo neighborPeerInfo;
	private Socket socket;

	private LinkedBlockingQueue<Message> msgStreamQueue;

	public PeerInfo getNeighborPeerInfo() {
		return neighborPeerInfo;
	}

	public void setNeighborPeerInfo(PeerInfo neighborPeerInfo) {
		this.neighborPeerInfo = neighborPeerInfo;
	}

	public Peer getPeer() {
		return peer;
	}

	public Client(final Peer peer, final PeerInfo neighborPeerInfo) {
		socket = null;
		try {
			socket = new Socket(neighborPeerInfo.getHostName(), neighborPeerInfo.getPortNumber());
		} catch (IOException e) {
		}
		initializeClient(peer, neighborPeerInfo, socket, new MessageStream(socket), false);
	}

	public Client(Peer peer, PeerInfo neighborPeerInfo, Socket socket, MessageStream msgStream) {
		initializeClient(peer, neighborPeerInfo, socket, msgStream, true);
	}

	private void initializeClient(Peer peer, PeerInfo neighborPeerInfo, Socket socket, MessageStream msgStream, boolean connectionEstablished) {
		this.connectionEstablished = connectionEstablished;
		this.isChoked = true;
		this.shutdown = false;
		this.downloadedPiecesSinceUnchoked = 0;
		this.lastDownloadRate = 0;

		this.msgStream = msgStream;
		this.peer = peer;
		this.neighborPeerInfo = neighborPeerInfo;
		this.socket = socket;

		this.msgStreamQueue = new LinkedBlockingQueue<>();
	}

}