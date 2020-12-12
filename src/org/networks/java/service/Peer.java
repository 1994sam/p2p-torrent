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

import static java.util.stream.Collectors.joining;

public class Peer {

    public boolean shutdown;
    public boolean fileDownloaded;
    public final int totalPieces;
    public final String fileDirPath;

    private ConcurrentHashMap<String, PeerInfo> peerInfoTable;
    private ConcurrentHashMap<String, PeerInfo> neighborPeerInfoTable;
    private ConcurrentHashMap<String, Client> neighborClientTable;
    private ConcurrentHashMap<String, HashSet<Integer>> pieceTracker;
    private ConcurrentHashMap<Integer, byte[]> pieces;

    public List<PeerInfo> runningPeerInfoList;

    public CommonConfig commonConfig;
    public PeerInfo peerInfo;

    private final ReadWriteLock bitLock = new ReentrantReadWriteLock();
    public FileHandler fileHandler;

    private final List<String> interestedPeers;

    private final Timer taskTimer;

    public Client previouslyUnchokedNeighbor;

    public Set<Integer> downloadedPieces;

    public Peer(final String peerId) throws IOException {
        fileDownloaded = false;
        shutdown = false;
        peerInfo = null;

        commonConfig = new CommonConfig();
        totalPieces = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());

        initializePeer(peerId);
        initializeTracker();
        updatePieceTracer(peerInfo);

        fileDirPath = Const.FILE_DIR_PREXFIX_PATH + peerId;
        fileHandler = new FileHandler(fileDirPath, peerInfo.isFilePresent(), commonConfig, peerId);
        pieces = new ConcurrentHashMap<>(fileHandler.getPieces());

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
        establishConnection();
        scheduleTasks();
    }

    private void initializePeer(final String peerId) {
        peerInfoTable = new ConcurrentHashMap<>();
        neighborPeerInfoTable = new ConcurrentHashMap<>(peerInfoTable);
        neighborClientTable = new ConcurrentHashMap<>();

        runningPeerInfoList = new ArrayList<>();

        for (PeerInfo curPeerInfo : new PeerConfig().getPeerInfo()) {
            curPeerInfo.setPieceIndexes(new BitSet(totalPieces));
            if (curPeerInfo.isFilePresent())
                curPeerInfo.getPieceIndexes().set(0, totalPieces);

            if (curPeerInfo.getPeerId().equals(peerId))
                peerInfo = curPeerInfo;
            else if (peerInfo == null) {
                runningPeerInfoList.add(curPeerInfo);
                peerInfoTable.put(curPeerInfo.getPeerId(), curPeerInfo);
            }
        }

        peerInfo.setMissingPieces(totalPieces - peerInfo.getPieceIndexes().cardinality());
        System.out.println(peerInfo.getPeerId() + " missing pieces " + peerInfo.getMissingPieces());
    }

    private void initializeTracker() {
        pieceTracker = new ConcurrentHashMap<>();
        downloadedPieces = new HashSet<>();

        BitSet bitSet = new BitSet(totalPieces);
        pieceTracker.put(peerInfo.getPeerId(), new HashSet<>());

        if (peerInfo.isFilePresent()) {
            bitSet.set(0, totalPieces);
            for (int i = 0; i < totalPieces; i++) {
                pieceTracker.get(peerInfo.getPeerId()).add(i);
                downloadedPieces.add(i);
            }
        }

        peerInfo.setPieceIndexes(bitSet);
    }

    private void runServer() {
        new Thread(new Server(this)).start();
    }

    private void establishConnection() {
        for (PeerInfo runningPeerInfo : runningPeerInfoList) {
            Client client = new Client(this, runningPeerInfo);
            new Thread(client).start();
        }
    }

    private void scheduleTasks() {
        taskTimer.schedule(new VerifyCompletionTask(this), 10000, 5000);
        taskTimer.schedule(new OptimisticUnchokingTask(this), 0, commonConfig.getOptimisticUnchokingInterval() * 100);
        //TODO: add task for preferred neighbor
    }

    public void updateNeighborPieceInfo(String peerID, Integer pieceIndex) {
        bitLock.writeLock().lock();

        try {
            PeerInfo neighborPeerInfo = peerInfoTable.get(peerID);
//            pieceTracker.get(peerID).remove(pieceIndex);
            neighborPeerInfo.getPieceIndexes().set(pieceIndex);
            neighborPeerInfo.setMissingPieces(neighborPeerInfo.getMissingPieces() - 1);
//            checkFileStatus(neighborPeerInfo);
	        checkFileStatus();
        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public boolean isPieceRequired(Integer pieceIndex) {
        bitLock.readLock().lock();
        try {
            return !downloadedPieces.contains(pieceIndex);
//            return pieceTracker.get(peerInfo.getPeerId()).contains(pieceIndex);
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public int getInterestedPieceIndex(String peerID) {
        try {
            bitLock.readLock().lock();

            HashSet<Integer> integers = getPieceTracker().get(peerID);

            if (integers == null || downloadedPieces == null) {
                return -1;
            }

            integers.removeAll(downloadedPieces);

            P2PLogger.getLogger().info("Neighbor Pieces = " + getPieceTracker().get(peerID).stream().sorted().map(String::valueOf).collect(joining(", ")));

            P2PLogger.getLogger().info("My Pieces = " + getPieceTracker().get(peerInfo.getPeerId()).stream().sorted().map(String::valueOf).collect(joining(", ")));

            P2PLogger.getLogger().info("Downloaded Pieces = " + downloadedPieces.stream().sorted().map(String::valueOf).collect(joining(", ")));

            if (!integers.isEmpty()) {
                return (int) integers.toArray()[new Random().nextInt(integers.size())];
            }

//            ArrayList<Integer> candidatePieces = new ArrayList<>(pieceTracker.get(peerID));
//            candidatePieces.removeAll(new ArrayList<>(pieceTracker.get(peerInfo.getPeerId())));
//            if (!candidatePieces.isEmpty()) {
//                return candidatePieces.get(new Random().nextInt(candidatePieces.size()));
//            }
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
                fileHandler.addFilePiece(data, pieceIndex);
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
        bitLock.readLock().lock();
        try {
            return new ArrayList<>(neighborClientTable.values());
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public void addNeighbor(final Client client) {
        bitLock.writeLock().lock();
        try {
            String neighborPeerId = client.neighborPeerInfo.getPeerId();
            peerInfoTable.put(neighborPeerId, client.neighborPeerInfo);
            neighborPeerInfoTable.put(neighborPeerId, client.neighborPeerInfo);
            neighborClientTable.put(neighborPeerId, client);
            pieceTracker.put(neighborPeerId, new HashSet<>());
            P2PLogger.getLogger().log(Level.INFO, "Client Table Info: " +
                neighborClientTable.keySet().stream().sorted().map(String::valueOf).collect(joining(", ")));
        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public boolean areAllNeighborsCompleted() {
        bitLock.readLock().lock();
        try {
            List<Client> completedPeers = new ArrayList<>();
            for (PeerInfo value : peerInfoTable.values()) {
                if (!value.isFilePresent()) {
                    return false;
                }
            }
//            Iterator<Map.Entry<String, HashSet<Integer>>> pieceIterator = pieceTracker.entrySet().iterator();
//            while (pieceIterator.hasNext()) {
//                Map.Entry<String, HashSet<Integer>> peer = pieceIterator.next();
//                if (peer.getKey().equals(peerInfo.getPeerId()))
//                    continue;
//
//                if (neighborClientTable.get(peer.getKey())) {
//                    completedPeers.add(neighborClientTable.get(peer.getKey()));
//                }
//                return completedPeers;
//            }
        } finally {
            bitLock.readLock().unlock();
        }
        return true;
    }

    public List<Client> getRunningClients() {
        bitLock.readLock().lock();
        List<Client> runningClients = new ArrayList<>();
        try {
            for (PeerInfo neighborPeerInfo: peerInfoTable.values()) {
                if (!neighborPeerInfo.isFilePresent()) {
                    runningClients.add(neighborClientTable.get(neighborPeerInfo.getPeerId()));
                }
            }
        } finally {
            bitLock.readLock().unlock();
        }
        return runningClients;
    }

    public ConcurrentHashMap<String, PeerInfo> getPeerInfoTable() {
        bitLock.readLock().lock();
        try {
            return peerInfoTable;
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public ConcurrentHashMap<String, HashSet<Integer>> getPieceTracker() {
        bitLock.readLock().lock();
        try {
            return pieceTracker;
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public ConcurrentHashMap<String, Client> getNeighborClientTable() {
        bitLock.readLock().lock();
        try {
            return neighborClientTable;
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public void shutdown() throws IOException {
        System.out.println(peerInfo.getPeerId() + " Shutting down!!___________________________________");
        neighborClientTable.forEach((key, value) -> value.shutdown());
//        if (!peerInfo.isFilePresent()) {
//            fileHandler.writeFileToDisk();
//        }
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        System.exit(0);
    }

    public Timer getTaskTimer() {
        return taskTimer;
    }

    public int getPieceTrackerSetSize(PeerInfo neighborPeerInfo) {
        int size = 0;
        bitLock.readLock().lock();

        try {
            size = pieceTracker.get(neighborPeerInfo.getPeerId()).size();
        } finally {
            bitLock.readLock().unlock();
        }

        return size;
    }

    public void updatePieceTracer(PeerInfo curPeerInfo) {
        bitLock.writeLock().lock();

        try {
            pieceTracker.computeIfAbsent(curPeerInfo.getPeerId(), k -> new HashSet<>());

            for (int i = 0; i < totalPieces; i++) {
                if (peerInfo.getPieceIndexes().get(i)) {
                    pieceTracker.get(peerInfo.getPeerId()).remove(i);
                    pieceTracker.get(curPeerInfo.getPeerId()).remove(i);
                } else {
                    pieceTracker.get(peerInfo.getPeerId()).add(i);
                    if (curPeerInfo.getPieceIndexes().get(i))
                        pieceTracker.get(curPeerInfo.getPeerId()).add(i);
                }
            }
//            curPeerInfo.setMissingPieces(pieceTracker.get(curPeerInfo.getPeerId()).size());

        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public void updatePiece(int pieceIndex, byte[] piece) {
        bitLock.writeLock().lock();

        try {
            pieces.put(pieceIndex, piece);
            peerInfo.getPieceIndexes().set(pieceIndex);
            peerInfo.setMissingPieces(peerInfo.getMissingPieces() - 1);
            fileHandler.getPieces().put(pieceIndex, piece);
            checkFileStatus();
//	        checkFileStatus(peerInfo);

        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public byte[] getPiece(int pieceIndex) {
        bitLock.readLock().lock();
        try {
            return pieces.get(pieceIndex);
        } finally {
            bitLock.readLock().unlock();
        }
    }

    public void checkFileStatus() {
        if (shutdown) return;
        bitLock.writeLock().lock();

        try {
            shutdown = checkFileStatus(peerInfo);
            for (PeerInfo neighborPeerInfo : peerInfoTable.values())
                shutdown &= checkFileStatus(neighborPeerInfo);

            if(peerInfo.isFilePresent() && !fileDownloaded && shutdown) {
                System.out.println("--------------- "  + fileDownloaded + " ---------------");
                P2PLogger.getLogger().log(Level.INFO, "--------------- "  + fileDownloaded + " ---------------");
                fileHandler.writeFileToDisk();
                fileDownloaded = true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public boolean checkFileStatus(PeerInfo curPeerInfo) {
        if (!curPeerInfo.isFilePresent())
            curPeerInfo.setFilePresent(curPeerInfo.getMissingPieces() == 0);
        return curPeerInfo.isFilePresent();
    }

}
