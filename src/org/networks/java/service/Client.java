package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;

public class Client implements Runnable {

    private boolean connected;
    private String msg;
    private String logMsg;
    final private CommonConfig commonConfig;
    private MessageStream msgStream;
    private final PeerInfo peerInfo;
    private PeerInfo neighborPeer;
    private final Peer peer;
    private List<PeerInfo> neighborPeers;

    public Client() {
        this(null, null, null, null);
    }

    public Client(final CommonConfig commonConfig, final PeerInfo peerInfo, final List<PeerInfo> neighborPeers, final Peer peer) {
        connected = false;
        msg = "";
        logMsg = "";
        this.commonConfig = commonConfig;
        this.peerInfo = peerInfo;
        neighborPeer = null;
        this.neighborPeers = neighborPeers;
        this.peer = peer;
    }

    @Override
    public void run() {

        neighborPeer = neighborPeers.get(0);

        while (true) {
            try {
                if (!connected) {
                    Socket socketCon = new Socket(neighborPeer.getHostName(), neighborPeer.getPortNum());
                    msgStream = new MessageStream(socketCon);
                    P2PLogger.getLogger().log(Level.INFO, "Peer is connected to " + neighborPeer.getHostName() + ":" + neighborPeer.getPortNum());
                    processHandshake();
                } else
                    processMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processHandshake() {
        logMsg = "Peer " + peerInfo.getPeerId() + " sending connection request to " + neighborPeer.getPeerId() + ".";
        P2PLogger.getLogger().log(Level.INFO, logMsg);
        msgStream.sendHandshakeMsg(peerInfo.getPeerId());

        while (!connected) {
            String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
            if (!neighborPeerId.isEmpty() && neighborPeer.getPeerId().equals(neighborPeerId)) {
                logMsg = "Peer " + peerInfo.getPeerId() + " makes a connection to " + neighborPeer.getPeerId() + ".";
                P2PLogger.getLogger().log(Level.INFO, logMsg);
                connected = true;
                processBitField();
//				msgStream.sendInterestedMsg(0);
//				msgStream.sendRequestMsg(15);
            }
        }
    }

    private void processBitField() {
        if (peerInfo.isFilePresent()) {
            msgStream.sendBitFieldMsg(306);
        }
    }

    private void processMessage() {
        msgStream.readMsg();
    }

    private void processPieceMessage() {

    }

    public void sendHaveMsg(int pieceIndex) {
        msgStream.sendHaveMsg(pieceIndex);

    }

}
