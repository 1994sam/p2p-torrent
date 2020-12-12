package org.networks.java.service;

import org.networks.java.helper.Constants;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
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
    public boolean isShutdown;
    private Socket socket;

    public Client(final Peer peer, final PeerInfo neighborPeerInfo) {
        socket = null;
        try {
            socket = new Socket(neighborPeerInfo.getHostName(), neighborPeerInfo.getPortNumber());
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
        int messageLength = msgStream.getInputStream().readInt();
        int messageType = Byte.toUnsignedInt(msgStream.getInputStream().readByte());
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
        if (msgStream.sendHandshakeMsg(peer.peerInfo.getPeerID())) {
            while (!connectionEstablished) {
                String neighborPeerId = msgStream.readHandshakeMsg(Constants.HANDSHAKE_MSG_LEN);
                if (!neighborPeerId.isEmpty() && neighborPeerInfo.getPeerID().equals(neighborPeerId)) {
                    connectionEstablished = true;
                    P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " makes a connection to " + neighborPeerInfo.getPeerID() + ".");
//                    peer.addNeighbor(this);
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
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " sending interested message to Peer " + neighborPeerInfo.getPeerID());
            msgStream.sendInterestedMsg(Constants.MSG_LEN_0);
        }

//        for (int pieceIndex : peer.getPieceTracker().get(neighborPeerInfo.getPeerId())) {
//            requestFileData(pieceIndex);
//        }
//        try {
//            peer.fileDownloadManager.writeFileToDisk();
//            peer.peerInfo.setFilePresent(true);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void processHaveMessage() throws IOException {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " received the `HAVE` message from Peer " + neighborPeerInfo.getPeerID());
        int pieceIndex = msgStream.getInputStream().readInt();
        peer.updateNeighborPieceInfo(neighborPeerInfo.getPeerID(), pieceIndex);

        if (peer.isPieceRequired(pieceIndex)) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " sending INTERESTED message to Peer " + neighborPeerInfo.getPeerID());
            msgStream.sendInterestedMsg(Constants.MSG_LEN_0);
        } else {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " sending NOT INTERESTED message to Peer " + neighborPeerInfo.getPeerID());
            msgStream.sendNotInterestedMsg(Constants.MSG_LEN_0);
        }
    }

    private void processUnChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerID() + " received Unchoke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
        unChokedAt = Instant.now();
        //TODO: send request for piece
        requestPiece();
    }

    private void requestPiece() {
        int pieceIndex = peer.getInterestedPieceIndex(neighborPeerInfo.getPeerID());
        if (pieceIndex == -1) {
            return;
        }
        requestFileData(pieceIndex);
    }

    private void processChokeMessage() {
        String logMsg = "Peer" + peer.peerInfo.getPeerID() + " received Choke message" + ".";
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);

        chokeTimeLock.writeLock().lock();
        //downloadRate = (float) (downloadedPieces / Duration.between(unChokedAt, Instant.now()).getSeconds());
        chokeTimeLock.writeLock().lock();
        downloadedPieces = 0;
    }

    private void processRequestMessage() throws IOException {
        if (!isChoked) {
            int pieceIndex = msgStream.getInputStream().readInt();
            P2PLogger.getLogger().log(Level.INFO, "Peer " + neighborPeerInfo.getPeerID() + " requested piece index " + pieceIndex + " to Peer " + peer.peerInfo.getPeerID());
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " sending piece at index " + pieceIndex + " to Peer " + neighborPeerInfo.getPeerID());
            msgStream.sendPieceMsg(pieceIndex, peer.getPiece(pieceIndex));
        }
    }

    private void processPieceMessage(int messageLength) throws IOException {
        int pieceIndex = msgStream.getInputStream().readInt();
        byte[] pieceMsg = msgStream.readPieceMsg(messageLength);
        boolean isAdded = peer.addFilePiece(pieceIndex, pieceMsg);
        downloadedPieces++;
        if (isAdded) {
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " downloaded piece: "
                    + pieceIndex + " from Peer " + neighborPeerInfo.getPeerID());
        }
        peer.downloadedPieces.add(pieceIndex);
        getNextFilePiece();
    }

    private void processInterestedMessage() {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " got INTERESTED message from Peer "
                + neighborPeerInfo.getPeerID());
        peer.addInterestedPeer(neighborPeerInfo.getPeerID());
    }

    private void processNotInterestedMessage() {
        P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " got NOT INTERESTED message from Peer "
                + neighborPeerInfo.getPeerID());
        peer.removeAsInterestedPeer(neighborPeerInfo.getPeerID());
    }

    private void getNextFilePiece() {
        int interestedPieceIndex = peer.getInterestedPieceIndex(peer.peerInfo.getPeerID());
        if (interestedPieceIndex != -1) {
            peer.getPieceTracker().get(neighborPeerInfo.getPeerID());
        }
    }

    public void sendHaveMessage(int pieceIndex) {
        for (Client client : peer.getNeighborClientTable().values())
            client.msgStream.sendHaveMsg(pieceIndex);
    }

    public float getDownloadRate() {
        return downloadRate;
    }

    public void chokeNeighbor() {
        if(!isShutdown) {
            isChoked = true;
            msgStream.sendChokeMsg(0);
        }
    }

    public void unchokeNeighbor() {
        if(!isShutdown) {
            isChoked = false;
            msgStream.sendUnChokeMsg(0);
        }
    }

    public void shutdown() {
        try {
            isShutdown = true;
            msgStream.getOutputStream().flush();
            if (socket != null) {
                msgStream.getInputStream().close();
                msgStream.getOutputStream().close();
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
            P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerID() + " requesting piece index " + pieceIndex + " to Peer " + neighborPeerInfo.getPeerID());
            msgStream.sendRequestMsg(Constants.PIECE_INDEX_PAYLOAD_LEN, pieceIndex);

            try {
                P2PLogger.getLogger().log(Level.INFO, "Peer " + neighborPeerInfo.getPeerID() + " sent piece at index " + pieceIndex + " to Peer " + peer.peerInfo.getPeerID());

                byte[] msgPayLoadLenBytes = new byte[Constants.MSG_LEN_LEN];
                msgStream.getInputStream().read(msgPayLoadLenBytes);
                int msgPayLoadLen = msgStream.byteArrayToInt(msgPayLoadLenBytes);
                int messageType = Byte.toUnsignedInt(msgStream.getInputStream().readByte());

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