package org.networks.java.service;

import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Client implements Runnable {

    private boolean connectionEstablished;

    private MessageStream msgStream;

    private Peer peer;
    private PeerInfo neighborPeerInfo;

    private int downloadedPieces;

    private Instant unChokedAt;
    private final ReadWriteLock chokeTimeLock = new ReentrantReadWriteLock();
    private float downloadRate;
    private boolean isChoked;
    private boolean isShutdown;
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

    public Client(final Peer peer, final PeerInfo neighborPeerInfo, final Socket socket, final MessageStream msgStream) {
        initializeClient(peer, neighborPeerInfo, socket, msgStream, true);
    }

    @Override
    public void run() {
        do {
            processHandShake();
        } while (!connectionEstablished);

        peer.neighborClientTable.put(neighborPeerInfo.getPeerId(), this);
        processBitFieldMessage();

        while (!isShutdown) {
            try {
                processMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeClient(final Peer peer, final PeerInfo neighborPeerInfo, final Socket socket, final MessageStream msgStream, final boolean connectionEstablished) {
        this.peer = peer;
        this.neighborPeerInfo = neighborPeerInfo;
        this.msgStream = msgStream;
        this.connectionEstablished = connectionEstablished;
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
                processBitFieldMessage(messageLength - 1);
                break;
            case 6:
                processRequestMessage();
                break;
            case 7:
                processPieceMessage(messageLength - 1);
                break;
        }
    }

    private void processHandShake() {
        if (msgStream.sendHandshakeMsg(peer.peerInfo.getPeerId())) {
            while (!connectionEstablished) {
                String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
                if (!neighborPeerId.isEmpty() && neighborPeerInfo.getPeerId().equals(neighborPeerId)) {
                    connectionEstablished = true;
                    peer.neighborPeerInfoTable.put(neighborPeerId, neighborPeerInfo);
                    P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " makes a connection to " + neighborPeerInfo.getPeerId() + ".");
                    peer.addNeighbor(this);
                }
            }
        }

    }

    private void processBitFieldMessage() {
        if (peer.peerInfo.getPieceIndexes().length() > 1)
            msgStream.sendBitFieldMsg(peer.peerInfo.getPieceIndexes());
    }

    private void processBitFieldMessage(int messageLength) {
        msgStream.readBitFieldMsg(neighborPeerInfo, messageLength); //reads the bit field message as well as sets the
        peer.pieceTracker.computeIfAbsent(neighborPeerInfo.getPeerId(), k -> new HashSet<>());

        peer.updatePieceTracer(neighborPeerInfo);
        if (peer.pieceTracker.get(neighborPeerInfo.getPeerId()).size() > 0) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending interested message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(0);
        }
    }

    private void processHaveMessage() throws IOException {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " received the `HAVE` message from Peer " + neighborPeerInfo.getPeerId());
        int pieceIndex = msgStream.getInStream().readInt();
        peer.updateNeighborPieceIndex(neighborPeerInfo.getPeerId(), pieceIndex);
        if (!peer.isPieceDownloaded(pieceIndex)) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending interested message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(0); //TODO: is this the correct way?
        }
    }

    private void processUnChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerId() + " received Unchoke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
        unChokedAt = Instant.now();
        requestPiece();
        //TODO: send request for piece
    }

    private void requestPiece() {
        int pieceIndex = peer.getInterestedPieceIndex(neighborPeerInfo.getPeerId());
        P2PLogger.getLogger().log(Level.INFO, "Sending request message to " + neighborPeerInfo.getPeerId());

        if (pieceIndex != -1) {
            msgStream.sendRequestMsg(0, pieceIndex);
        }
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
        int pieceIndex = msgStream.getInStream().readInt();
        if (!isChoked) {
            byte[] filePiece = peer.getFilePiece(pieceIndex);
            if (filePiece != null) {
                msgStream.sendPieceMsg(pieceIndex, filePiece);
            }
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
            msgStream.sendRequestMsg(0, interestedPieceIndex); //TODO: is this the correct way?
        }
    }

    public void sendHaveMessage(int pieceIndex) {
        msgStream.sendHaveMsg(pieceIndex);
    }

    public float getDownloadRate() {
        return downloadRate;
    }

    public void chokeNeighbor() {
        isChoked = true;
        msgStream.sendChokeMsg(1);
    }

    public void unchokeNeighbor() {
        isChoked = false;
        msgStream.sendUnChokeMsg(0);
    }

    public void shutdown() {
        try {
            isShutdown = true;
            msgStream.getOutStream().flush();
            socket.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    public Peer getPeer() {
        return peer;
    }

}
