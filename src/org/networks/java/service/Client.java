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
    public PeerInfo neighborPeerInfo;

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

        peer.addNeighbor(this);
        processBitFieldMessage();

        while (!isShutdown) {
            try {
                processMessage();
            } catch (IOException e) {
//                e.printStackTrace();
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
//                processHaveMessage();
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
        peer.updatePieceTracer(neighborPeerInfo);

        if (peer.getPieceTrackerSetSize(neighborPeerInfo) > 0) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending interested message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(0);
        }

        for (int pieceIndex : peer.getPieceTracker().get(neighborPeerInfo.getPeerId())) {
            requestFileData(pieceIndex);
        }
        try {
            peer.fileHandler.writeFileToDisk();
            peer.peerInfo.setFilePresent(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processHaveMessage() throws IOException {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " received the `HAVE` message from Peer " + neighborPeerInfo.getPeerId());
        int pieceIndex = msgStream.getInStream().readInt();
        peer.updateNeighborPieceIndex(neighborPeerInfo.getPeerId(), pieceIndex);
        if (peer.isPieceRequired(pieceIndex)) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending interested message to Peer " + neighborPeerInfo.getPeerId());
            msgStream.sendInterestedMsg(0);
        }
    }

    private void processUnChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerId() + " received Unchoke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
        unChokedAt = Instant.now();
        //TODO: send request for piece
    }

    private void processChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerId() + " received Choke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);

        chokeTimeLock.writeLock().lock();
        downloadRate = (float) (downloadedPieces / Duration.between(unChokedAt, Instant.now()).getSeconds());
        chokeTimeLock.writeLock().lock();
        downloadedPieces = 0;
    }

    private void processRequestMessage() throws IOException {
        int pieceIndex = msgStream.getInStream().readInt();
//        int pieceIndex = msgStream.readRequestMsg(Const.PIECE_INDEX_PAYLOAD_LEN);
        P2PLogger.getLogger().log(Level.INFO, "Peer " + neighborPeerInfo.getPeerId() + " requested piece index " + pieceIndex + " to Peer " + peer.peerInfo.getPeerId());
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " sending piece at index " + pieceIndex + " to Peer " + neighborPeerInfo.getPeerId());
        msgStream.sendPieceMsg(pieceIndex, peer.getPiece(pieceIndex));

//        P2PLogger.getLogger().log(Level.INFO,pieceIndex + " ---");
//        P2PLogger.getLogger().log(Level.INFO, new String(peer.pieces.get(pieceIndex)));

//        if (!isChoked) {
//            byte[] filePiece = peer.getFilePiece(pieceIndex);
//            if (filePiece != null) {
//                msgStream.sendPieceMsg(pieceIndex, filePiece);
//            }
//        }
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
            msgStream.sendRequestMsg(Const.PIECE_INDEX_PAYLOAD_LEN, interestedPieceIndex);
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
        msgStream.sendUnChokeMsg(1);
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

//                System.out.println(msgPayLoadLen);
                byte[] pieces = msgStream.readPieceMsg(msgPayLoadLen);

//                P2PLogger.getLogger().log(Level.INFO,pieceIndex + " ---");
//                P2PLogger.getLogger().log(Level.INFO, new String(pieces));

                peer.updatePiece(pieceIndex, pieces);
                peer.fileHandler.getPieces().put(pieceIndex, pieces);
//                peer.fileHandler.addFilePiece(pieces, pieceIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}