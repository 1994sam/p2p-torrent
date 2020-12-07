package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

public class ClientOld implements Runnable {

    private boolean connected;
    private String msg;
    private String logMsg;
    final private CommonConfig commonConfig;
    private MessageStream msgStream;
    private final PeerInfo peerInfo;
    private PeerInfo serverPeerInfo;

    public ClientOld() {
        this(null, null, null);
    }

    public ClientOld(final CommonConfig commonConfig, final PeerInfo peerInfo, final PeerInfo serverPeerInfo) {
        connected = false;
        msg = "";
        logMsg = "";
        this.commonConfig = commonConfig;
        this.peerInfo = peerInfo;
        this.serverPeerInfo = serverPeerInfo;
    }

    @Override
    public void run() {

        while (true) {
            try {
                if (!connected) {
                    Socket socketCon = new Socket(serverPeerInfo.getHostName(), serverPeerInfo.getPortNum());
                    msgStream = new MessageStream(socketCon);
                    processHandshake();
                } else {

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processHandshake() {
        if(msgStream.sendHandshakeMsg(peerInfo.getPeerId())) {
            while (!connected) {
                String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
                if (!neighborPeerId.isEmpty() && serverPeerInfo.getPeerId().equals(neighborPeerId)) {
                    connected = true;
                    logMsg = "Peer " + peerInfo.getPeerId() + " makes a connection to " + serverPeerInfo.getPeerId() + ".";
                    P2PLogger.getLogger().log(Level.INFO, logMsg);
                    processBitField();
                }
            }
        }


    }

    private void processBitField() {
        if (peerInfo.getPieceIndexes().length() > 0) {
            while(!msgStream.sendBitFieldMsg(peerInfo.getPieceIndexes()));
        }
        processMessage();
    }

    private void processMessage() {
        msgStream.readMsg(serverPeerInfo);
    }

    private void processPieceMessage() {

    }

    public void sendHaveMsg(int pieceIndex) {
        msgStream.sendHaveMsg(pieceIndex);

    }

}
