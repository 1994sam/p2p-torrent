package org.networks.java.service;

import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.HandshakeMessage;
import org.networks.java.model.Message;
import org.networks.java.model.MessageType;
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

public class Client implements Runnable {

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

    public Client(final Peer peer, final PeerInfo neighborPeerInfo) {
        socket = null;
        try {
            socket = new Socket(neighborPeerInfo.getHostName(), neighborPeerInfo.getPortNum());
        } catch (IOException e) {
            e.printStackTrace();
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
			System.out.println("Terminating connection with Peer " + neighborPeerInfo.getPeerId());
		} catch (Exception ex) {
		}
	}

    private void processMessage() throws IOException {
        int messageLength = msgStream.getInStream().readInt();
        int messageType = Byte.toUnsignedInt(msgStream.getInStream().readByte());
        switch (messageType) {
            case 0:
                processChokeMessage();
                break;
            case 1:
                processUnChokeMessage();
                break;
            case 2:
                processInterestedMessage();
                break;
            case 3:
                processNotInterestedMessage();
                break;
            case 4:
                processHaveMessage();
                break;
            case 5:
                processBitFieldMessage(messageLength);
                break;
            case 6:
                processRequestMessage();
                break;
            case 7:
                processPieceMessage(messageLength);
                break;
        }
    }

    private void processHandShake() {
        if (msgStream.sendHandshakeMsg(peer.peerInfo.getPeerId())) {
            while (!connectionEstablished) {
                String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
                if (!neighborPeerId.isEmpty() && neighborPeerInfo.getPeerId().equals(neighborPeerId)) {
                    connectionEstablished = true;
                    P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " makes a connection to " + neighborPeerInfo.getPeerId() + ".");
//                    peer.addNeighbor(this);
                }
            }
        }

	private void pushMsgToQueue() throws InterruptedException, IOException {
		while (true) {
			Message msg = msgStreamQueue.take();
			if (msg.getMsgType() == null)
				return;
			sendMsg(msg);
		}
	}

	private void readChokeMsg() throws IOException {
		P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.getPeerInfo().getPeerId() + " is choked by Peer " + neighborPeerInfo.getPeerId());

        if (peer.getPieceTrackerSetSize(neighborPeerInfo) > 0) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending interested message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(Const.MSG_LEN_0);
        }

//        for (int pieceIndex : peer.getPieceTracker().get(neighborPeerInfo.getPeerId())) {
//            requestFileData(pieceIndex);
//        }
//        try {
//            peer.fileHandler.writeFileToDisk();
//            peer.peerInfo.setFilePresent(true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void processHaveMessage() throws IOException {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " received the `HAVE` message from Peer " + neighborPeerInfo.getPeerId());
        int pieceIndex = msgStream.getInStream().readInt();
        peer.updateNeighborPieceInfo(neighborPeerInfo.getPeerId(), pieceIndex);

        if (peer.isPieceRequired(pieceIndex)) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending INTERESTED message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(Const.MSG_LEN_0);
        } else {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending NOT INTERESTED message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendNotInterestedMsg(Const.MSG_LEN_0);
        }
    }

    private void processUnChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerId() + " received Unchoke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
        unChokedAt = Instant.now();
        //TODO: send request for piece
        requestPiece();
    }

    private void requestPiece() {
        int pieceIndex = peer.getInterestedPieceIndex(neighborPeerInfo.getPeerId());
        if (pieceIndex == -1) {
            return;
        }
        requestFileData(pieceIndex);
    }

    private void processChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerId() + " received Choke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);

        chokeTimeLock.writeLock().lock();
        //downloadRate = (float) (downloadedPieces / Duration.between(unChokedAt, Instant.now()).getSeconds());
        chokeTimeLock.writeLock().lock();
        downloadedPieces = 0;
    }

    private void processRequestMessage() throws IOException {
        if (!isChoked) {
            int pieceIndex = msgStream.getInStream().readInt();
            P2PLogger.getLogger().log(Level.INFO, "Peer " + neighborPeerInfo.getPeerId() + " requested piece index " + pieceIndex + " to Peer " + peer.peerInfo.getPeerId());
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending piece at index " + pieceIndex + " to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendPieceMsg(pieceIndex, peer.getPiece(pieceIndex));
        }
    }

    private void processPieceMessage(int messageLength) throws IOException {
        int pieceIndex = msgStream.getInStream().readInt();
        byte[] pieceMsg = msgStream.readPieceMsg(messageLength);
        boolean isAdded = peer.addFilePiece(pieceIndex, pieceMsg);
        downloadedPieces++;
        if (isAdded) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " downloaded piece: "
                    + pieceIndex + " from Peer " + neighborPeerInfo.getPeerId());
        }
        peer.downloadedPieces.add(pieceIndex);
        getNextFilePiece();
    }

    private void processInterestedMessage() {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " got INTERESTED message from Peer "
                + neighborPeerInfo.getPeerId());
        peer.addInterestedPeer(neighborPeerInfo.getPeerId());
    }

    private void processNotInterestedMessage() {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " got NOT INTERESTED message from Peer "
                + neighborPeerInfo.getPeerId());
        peer.removeAsInterestedPeer(neighborPeerInfo.getPeerId());
    }

    private void getNextFilePiece() {
        int interestedPieceIndex = peer.getInterestedPieceIndex(peer.peerInfo.getPeerId());
        if (interestedPieceIndex != -1) {
            peer.getPieceTracker().get(neighborPeerInfo.getPeerId());
        }
    }

	public void sendBitFieldMsg() throws IOException {
		if (peer.hasOnePiece()) {
			Message msg = new Message(MessageType.BITFIELD, peer.getBitField());
			sendMsg(msg);
		}
	}

	public void sendHaveMsg(int pieceIndex) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(pieceIndex);
		Message msg = new Message(MessageType.HAVE, byteBuffer.array());
		msgStreamQueue.add(msg);
	}

	public void chokeNeighbor() {
		isChoked = true;
		Message msg = new Message(MessageType.CHOKE, null);
		msgStreamQueue.add(msg);
	}

	public void unchokeNeighbor() {
		isChoked = false;
		Message msg = new Message(MessageType.UNCHOKE, null);
		msgStreamQueue.add(msg);
	}

	private void requestPiece() {
		int pieceIndex = peer.getPieceRequestIndex(neighborPeerInfo.getPeerId());
		if (pieceIndex == -1)
			return;
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(pieceIndex);
		Message msg = new Message(MessageType.REQUEST, byteBuffer.array());
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
            isShutdown = true;
            msgStream.getOutStream().flush();
            if (socket != null) {
                msgStream.getInStream().close();
                msgStream.getOutStream().close();
                socket.close();
            }
        } catch (IOException e) {
            // Do nothing
        }
    }

    public Peer getPeer() {
        return peer;
    }

    public void requestFileData(int pieceIndex) {
        if (peer.isPieceRequired(pieceIndex)) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " requesting piece index " + pieceIndex + " to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendRequestMsg(Const.PIECE_INDEX_PAYLOAD_LEN, pieceIndex);

            try {
                P2PLogger.getLogger().log(Level.INFO, "Peer " + neighborPeerInfo.getPeerId() + " sent piece at index " + pieceIndex + " to Peer " + peer.peerInfo.getPeerId());

                byte[] msgPayLoadLenBytes = new byte[Const.MSG_LEN_LEN];
                msgStream.getInStream().read(msgPayLoadLenBytes);
                int msgPayLoadLen = msgStream.byteArrayToInt(msgPayLoadLenBytes);
                int messageType = Byte.toUnsignedInt(msgStream.getInStream().readByte());

                byte[] piece = msgStream.readPieceMsg(msgPayLoadLen);
                peer.updatePiece(pieceIndex, piece);

                peer.downloadedPieces.add(pieceIndex);

//                sendHaveMessage(pieceIndex);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}