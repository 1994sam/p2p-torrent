package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.PeerConfig;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class Peer {

    public int totalPieces;

    public ConcurrentHashMap<String, PeerInfo> peerInfoTable;
    public ConcurrentHashMap<String, PeerInfo> neighborPeerInfoTable;
    public ConcurrentHashMap<String, Client> neighborClientTable;

    public CommonConfig commonConfig;
    public PeerInfo peerInfo;

    public Peer(final String peerId) {
        initializePeer(peerId);
        P2PLogger.setLogger(peerInfo.getPeerId());
    }

    public void run() {
        P2PLogger.getLogger().log(Level.INFO, "Starting peer process: " + peerInfo.getPeerId());
        runServer();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateNeighbors();
        establishConnection();
    }

    private void initializePeer(final String peerId) {
        peerInfoTable = new ConcurrentHashMap<>();
        neighborPeerInfoTable = new ConcurrentHashMap<>();
        neighborClientTable = new ConcurrentHashMap<>();

        commonConfig = new CommonConfig();
        totalPieces = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());

        for(PeerInfo curPeerInfo: new PeerConfig().getPeerInfo()) {
            curPeerInfo.setPieceIndexes(new BitSet(totalPieces));
            if (curPeerInfo.isFilePresent())
                curPeerInfo.getPieceIndexes().set(0, totalPieces);
            if(curPeerInfo.getPeerId().equals(peerId))
                this.peerInfo = curPeerInfo;
            else
                peerInfoTable.put(curPeerInfo.getPeerId(), curPeerInfo);

        }
    }

    private void runServer() {
        new Thread(new Server(this)).start();
    }

    private void updateNeighbors() {
        neighborPeerInfoTable = new ConcurrentHashMap<>(peerInfoTable);
    }

    private void establishConnection() {
        for(Map.Entry<String, PeerInfo> neighborPeerInfoEntry: neighborPeerInfoTable.entrySet()) {
            Client client = new Client(this, neighborPeerInfoEntry.getValue());
            new Thread(client).start();
        }
    }
}
