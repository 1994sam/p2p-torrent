package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.FileHandler;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class Peer {

    final private CommonConfig commonConfig;
    final private PeerInfo peerInfo;
    private List<PeerInfo> neighborPeers;
    private ConcurrentHashMap<String, Client> neighbors;

    private final ConcurrentHashMap<String, HashSet<Integer>> pieceTracker;

    private BitSet piecesBitSet;
    private final ReadWriteLock piecesBitSetLock = new ReentrantReadWriteLock();

    private int totalPieces;

    private final FileHandler fileHandler;

    public Peer(final CommonConfig commonConfig, final PeerInfo peerInfo, final List<PeerInfo> neighborPeers) throws IOException {
        this.commonConfig = commonConfig;
        this.peerInfo = peerInfo;
        this.neighborPeers = neighborPeers;

        pieceTracker = new ConcurrentHashMap<>();
        pieceTracker.put(peerInfo.getPeerId(), new HashSet<>());

        totalPieces = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());
        piecesBitSet = new BitSet(totalPieces);
        if (peerInfo.isFilePresent()) {
            piecesBitSet.set(0, totalPieces);
            for (int i = 0; i < totalPieces; i++)
                pieceTracker.get(peerInfo.getPeerId()).add(i);
        }

        fileHandler = new FileHandler(commonConfig.getFileName(), peerInfo.isFilePresent(), commonConfig);

        P2PLogger.setLogger(peerInfo.getPeerId());
    }

    public void run() {
        P2PLogger.getLogger().log(Level.INFO, "Starting peer process");
        new Thread(new Client(commonConfig, peerInfo, neighborPeers, this)).start();
        new Thread(new Listener(commonConfig, peerInfo)).start();
    }

    public byte[] getFilePiece(Integer pieceIndex) throws IOException {
        byte[] piece = null;
        piecesBitSetLock.readLock().lock();
        try {
            if (piecesBitSet.get(pieceIndex)) {
                piece = fileHandler.getFilePiece(pieceIndex);
            }
        } finally {
            piecesBitSetLock.readLock().unlock();
        }
        return piece;
    }

    public boolean addPiece(Integer pieceIndex, byte[] data) throws IOException {
        piecesBitSetLock.writeLock().lock();
        try {
            if (pieceTracker.get(peerInfo.getPeerId()).contains(pieceIndex))
                return false;

            if (!piecesBitSet.get(pieceIndex)) {
                fileHandler.addFilePiece(data, pieceIndex, peerInfo.getPeerId());
                piecesBitSet.set(pieceIndex);
                pieceTracker.get(peerInfo.getPeerId()).add(pieceIndex);
                if (piecesBitSet.nextClearBit(0) >= totalPieces) {
                    P2PLogger.getLogger().info("Peer " + peerInfo.getPeerId() + " has finished downloading the file.");
                }
            }
        } finally {
            piecesBitSetLock.writeLock().unlock();
        }

        for (Map.Entry<String, Client> pair : neighbors.entrySet()) {
            pair.getValue().sendHaveMsg(pieceIndex);
        }
        return true;
    }

    private void shutdown() throws IOException {
        fileHandler.writeFileToDisk();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }
}
