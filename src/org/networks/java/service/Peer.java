package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.Const;
import org.networks.java.helper.FileHandler;
import org.networks.java.helper.PeerConfig;
import org.networks.java.model.PeerInfo;
import org.networks.java.tasks.OptimisticUnchokingTask;
import org.networks.java.tasks.VerifyCompletionTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Peer {

    public final int totalPieces;
    public final String fileDirPath;

    public ConcurrentHashMap<String, PeerInfo> peerInfoTable;
    public ConcurrentHashMap<String, PeerInfo> neighborPeerInfoTable;
    public ConcurrentHashMap<String, Client> neighborClientTable;

    public ConcurrentHashMap<String, HashSet<Integer>> pieceTracker;

    public CommonConfig commonConfig;
    public PeerInfo peerInfo;

    private final ReadWriteLock bitLock = new ReentrantReadWriteLock();
    private FileHandler fileHandler;

    private final List<String> interestedPeers;

    private final Timer taskTimer;

    public Peer(final String peerId) throws IOException {
        commonConfig = new CommonConfig();
        totalPieces = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());

        initializePeer(peerId);

        fileDirPath = Const.FILE_DIR_PREXFIX_PATH + peerId;
        fileHandler = new FileHandler(fileDirPath, peerInfo.isFilePresent(), commonConfig, peerId);

        pieceTracker = new ConcurrentHashMap<>();
        interestedPeers = Collections.synchronizedList(new ArrayList<>());
        taskTimer = new Timer(true);
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
        scheduleTasks();
    }

    private void scheduleTasks() {
        taskTimer.schedule(new OptimisticUnchokingTask(this), 0, commonConfig.getOptimisticUnchokingInterval());
        taskTimer.schedule(new VerifyCompletionTask(this), 10000, 5000);
        //TODO: add task for preferred neighbor
    }

    private void initializePeer(final String peerId) {
        peerInfoTable = new ConcurrentHashMap<>();
        neighborPeerInfoTable = new ConcurrentHashMap<>();
        neighborClientTable = new ConcurrentHashMap<>();

        for (PeerInfo curPeerInfo : new PeerConfig().getPeerInfo()) {
            curPeerInfo.setPieceIndexes(new BitSet(totalPieces));
            if (curPeerInfo.isFilePresent())
                curPeerInfo.getPieceIndexes().set(0, totalPieces);
            if (curPeerInfo.getPeerId().equals(peerId))
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
        for (Map.Entry<String, PeerInfo> neighborPeerInfoEntry : neighborPeerInfoTable.entrySet()) {
            Client client = new Client(this, neighborPeerInfoEntry.getValue());
            new Thread(client).start();
        }
    }

    public void updateNeighborPieceIndex(String peerID, Integer pieceIndex) {
        try {
            bitLock.writeLock().lock();
            BitSet nBitSet = neighborPeerInfoTable.get(peerID).getPieceIndexes();
            nBitSet.set(pieceIndex);
            pieceTracker.get(peerID).add(pieceIndex);
        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public boolean isPieceDownloaded(Integer pieceIndex) {
        bitLock.readLock().lock();
        try {
            return pieceTracker.get(peerInfo.getPeerId()).contains(pieceIndex);
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public int getInterestedPieceIndex(String peerID) {
        try {
            bitLock.readLock().lock();
            ArrayList<Integer> candidatePieces = new ArrayList<>(pieceTracker.get(peerID));
            candidatePieces.removeAll(new ArrayList<>(pieceTracker.get(peerInfo.getPeerId())));
            if (!candidatePieces.isEmpty()) {
                return candidatePieces.get(new Random().nextInt(candidatePieces.size()));
            }
        } finally {
            bitLock.readLock().unlock();
        }
        return -1;
    }

    public boolean addFilePiece(Integer pieceIndex, byte[] data) throws IOException {
        bitLock.writeLock().lock();
        try {
            if (pieceTracker.get(peerInfo.getPeerId()).contains(pieceIndex)) {
                return false;
            }

            if (!peerInfo.getPieceIndexes().get(pieceIndex)) {
                fileHandler.addFilePiece(data, pieceIndex, peerInfo.getPeerId());
                peerInfo.getPieceIndexes().set(pieceIndex);
                pieceTracker.get(peerInfo.getPeerId()).add(pieceIndex);
                if (peerInfo.getPieceIndexes().nextClearBit(0) >= totalPieces) {
                    P2PLogger.getLogger().info("Peer " + peerInfo.getPeerId() + " has downloaded the complete file.");
                }
            }
        } finally {
            bitLock.writeLock().unlock();
        }

        for (Map.Entry<String, Client> entry : neighborClientTable.entrySet()) {
            entry.getValue().sendHaveMessage(pieceIndex);
        }

        return true;
    }

    public byte[] getFilePiece(int pieceIndex) throws IOException {
        bitLock.readLock().lock();
        try {
            if (peerInfo.getPieceIndexes().get(pieceIndex)) {
                return fileHandler.getFilePiece(pieceIndex);
            }
        } finally {
            bitLock.readLock().unlock();
        }
        return null;
    }

    public void addInterestedPeer(String peerID) {
        synchronized (interestedPeers) {
            interestedPeers.add(peerID);
        }
    }

    public void removeAsInterestedPeer(String peerID) {
        synchronized (interestedPeers) {
            interestedPeers.remove(peerID);
        }
    }

    public List<Client> getNeighbors() {
        return new ArrayList<>(neighborClientTable.values());
    }

    public List<Client> getPeersFinished() {
        bitLock.readLock().lock();
        try {
            List<Client> completedPeers = new ArrayList<>();
            Iterator<Map.Entry<String, HashSet<Integer>>> pieceIterator = pieceTracker.entrySet().iterator();
            while (pieceIterator.hasNext()) {
                Map.Entry<String, HashSet<Integer>> peer = pieceIterator.next();
                if (peer.getKey().equals(peerInfo.getPeerId()))
                    continue;

                if (peer.getValue().size() == totalPieces) {
                    completedPeers.add(neighborClientTable.get(peer.getKey()));
                }
                return completedPeers;
            }
        } finally {
            bitLock.readLock().unlock();
        }
        return new ArrayList<>();
    }

    public ConcurrentHashMap<String, HashSet<Integer>> getPieceTracker() {
        return pieceTracker;
    }

    public void shutdown() throws IOException {
        neighborClientTable.forEach((key, value) -> value.shutdown());
        fileHandler.writeFileToDisk();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public Timer getTaskTimer() {
        return taskTimer;
    }

    public void updatePieceTracer(PeerInfo neighborPeerInfo) {
        for(int i = 0; i < totalPieces; i++) {
            if(peerInfo.getPieceIndexes().get(i))
                pieceTracker.get(neighborPeerInfo.getPeerId()).remove(i);
            else if(neighborPeerInfo.getPieceIndexes().get(i))
                pieceTracker.get(neighborPeerInfo.getPeerId()).add(i);
        }
        System.out.println(pieceTracker.get(neighborPeerInfo.getPeerId()).toString());
    }

}
