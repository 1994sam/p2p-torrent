package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.Constants;
import org.networks.java.helper.FileDownloader;
import org.networks.java.model.PeerInfo;
import org.networks.java.tasks.GeneratePreferredNeighbors;
import org.networks.java.tasks.OptimisticUnchokingTask;
import org.networks.java.tasks.VerifyCompletionTask;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Peer {

    private final PeerInfo peerInfo;

    private int numberOfPiecesToBeDownloaded;
    private final ReadWriteLock lock;

    private final ConcurrentHashMap<String, BitSet> neighbourPieceIndices = new ConcurrentHashMap<>();
    private BitSet piecesIndex;

    public ConcurrentHashMap<String, HashSet<Integer>> getPieceIndexStore() {
        return pieceIndexStore;
    }

    private final ConcurrentHashMap<String, HashSet<Integer>> pieceIndexStore;

    private final ConcurrentHashMap<String, Client> peerIdToNeighbourClientMapping;
    private List<Client> neighboursPreferred;
    private final List<String> peersInterestedInMe;

    private Client previouslyUnchokedClient;

    private final FileDownloader fileDownloader;

    private final Timer taskTimer = new Timer(true);

    private final CommonConfig commonConfig = new CommonConfig();

    private final List<PeerInfo> neighbours;

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public Peer(PeerInfo peerInfo, List<PeerInfo> neighbours) throws IOException {
        this.peerInfo = peerInfo;
        peerIdToNeighbourClientMapping = new ConcurrentHashMap<>();
        neighboursPreferred = new ArrayList<>();
        peersInterestedInMe = Collections.synchronizedList(new ArrayList<>());
        pieceIndexStore = new ConcurrentHashMap<>();
        pieceIndexStore.put(peerInfo.getPeerId(), new HashSet<>());

        initializeTracker(peerInfo);

        fileDownloader = new FileDownloader(commonConfig.getFileName(), peerInfo.isFilePresent(), commonConfig, peerInfo.getPeerId());
        lock = new ReentrantReadWriteLock();
        this.neighbours = neighbours;
        P2PLogger.setLogger(peerInfo.getPeerId());
    }

    private int initializeTracker(PeerInfo peerInfo) {
        numberOfPiecesToBeDownloaded = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());
        piecesIndex = new BitSet(numberOfPiecesToBeDownloaded);
        if (peerInfo.isFilePresent()) {
            piecesIndex.set(0, numberOfPiecesToBeDownloaded);
            for (int i = 0; i < numberOfPiecesToBeDownloaded; i++)
                pieceIndexStore.get(peerInfo.getPeerId()).add(i);
        }
        return numberOfPiecesToBeDownloaded;
    }

    public boolean hasOnePiece() {
        return pieceIndexStore.get(peerInfo.getPeerId()).size() > 0;
    }

    public void addClient(Client client) {
        String peerId = client.getNeighborPeerInfo().getPeerId();
        neighbourPieceIndices.put(peerId, new BitSet(piecesIndex.size()));
        pieceIndexStore.computeIfAbsent(peerId, k -> new HashSet<>());
        peerIdToNeighbourClientMapping.put(peerId, client);
    }

    public void start() {
        new Thread(new Server(this)).start();
        neighbours.forEach(peerInfo -> {
            new Thread(new Client(this, peerInfo)).start();
        });
        scheduleTasks();
    }

    private void scheduleTasks() {
        taskTimer.schedule(new VerifyCompletionTask(this), 10 * Constants.SEC_TO_MILLI_SEC, 5 * Constants.SEC_TO_MILLI_SEC);
        taskTimer.schedule(new GeneratePreferredNeighbors(this), 0, commonConfig.getUnchokingInterval() * Constants.SEC_TO_MILLI_SEC);
        taskTimer.schedule(new OptimisticUnchokingTask(this), 0, commonConfig.getOptimisticUnchokingInterval() * Constants.SEC_TO_MILLI_SEC);
    }

    private List<Client> getPeersToSendData() {
        List<Client> completedPeers = getCompletedPeers();
        List<Client> candidatePeers = new ArrayList<>(peerIdToNeighbourClientMapping.values());
        candidatePeers.removeAll(completedPeers);
        if (previouslyUnchokedClient != null) {
            candidatePeers.remove(previouslyUnchokedClient);
        }
        return candidatePeers;
    }

    public List<Client> getCompletedPeers() {
        lock.readLock().lock();
        List<Client> completedPeers = new ArrayList<>();
        Iterator<Entry<String, HashSet<Integer>>> pieceIterator = pieceIndexStore.entrySet().iterator();
        while (pieceIterator.hasNext()) {
            Map.Entry<String, HashSet<Integer>> peer = pieceIterator.next();
            if (peer.getKey().equals(peerInfo.getPeerId()))
                continue;

            if (peer.getValue().size() == numberOfPiecesToBeDownloaded)
                completedPeers.add(peerIdToNeighbourClientMapping.get(peer.getKey()));
        }
        lock.readLock().unlock();
        return completedPeers;
    }

    public void updateNeighborPieceIndex(String peerID, Integer pieceIndex) {
        lock.writeLock().lock();
        try {
            BitSet nBitSet = neighbourPieceIndices.get(peerID);
            nBitSet.set(pieceIndex);
            neighbourPieceIndices.put(peerID, nBitSet);
            pieceIndexStore.get(peerID).add(pieceIndex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setNeighborBitField(String peerID, byte[] bitField) {
        try {
            lock.writeLock().lock();
//            BitSet bitSet = getBitSet(bitField);
            BitSet bitSet = BitSet.valueOf(bitField);
            neighbourPieceIndices.put(peerID, bitSet);
            for (int i = 0; i < numberOfPiecesToBeDownloaded; i++) {
                if (bitSet.get(i)) {
                    pieceIndexStore.get(peerID).add(i);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasPiece(Integer pieceIndex) {
        try {
            lock.readLock().lock();
            return pieceIndexStore.get(peerInfo.getPeerId()).contains(pieceIndex);
        } finally {
            lock.readLock().unlock();
        }
    }

    public byte[] getBitField() {
        lock.readLock().lock();
        byte[] bytes;
        try {
            bytes = piecesIndex.toByteArray();
        } finally {
            lock.readLock().unlock();
        }
        return bytes;
    }

    public int getPieceRequestIndex(String peerID) {
        try {
            lock.readLock().lock();
            ArrayList<Integer> candidatePieces = new ArrayList<>(pieceIndexStore.get(peerID));
            candidatePieces.removeAll(new ArrayList<>(pieceIndexStore.get(peerInfo.getPeerId())));
            if (!candidatePieces.isEmpty()) {
                return candidatePieces.get(new Random().nextInt(candidatePieces.size()));
            }
        } finally {
            lock.readLock().unlock();
        }
        return -1;
    }

    public boolean addPiece(Integer pieceIndex, byte[] data) {
        lock.writeLock().lock();
        try {
            if (pieceIndexStore.get(peerInfo.getPeerId()).contains(pieceIndex))
                return false;

            if (!piecesIndex.get(pieceIndex)) {
                fileDownloader.addFilePiece(data, pieceIndex);
                piecesIndex.set(pieceIndex);
                pieceIndexStore.get(peerInfo.getPeerId()).add(pieceIndex);
                if (piecesIndex.nextClearBit(0) >= numberOfPiecesToBeDownloaded) {
                    P2PLogger.getLogger().log(Level.INFO, "Peer " + peerInfo.getPeerId() + " has downloaded the complete file.");
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        peerIdToNeighbourClientMapping.values().forEach(client -> client.sendHaveMsg(pieceIndex));

        return true;
    }

    public byte[] getPiece(Integer pieceIndex) {
        byte[] piece = null;
        lock.readLock().lock();
        try {
            if (piecesIndex.get(pieceIndex)) {
                piece = fileDownloader.getFilePiece(pieceIndex);
            }
        } finally {
            lock.readLock().unlock();
        }
        return piece;
    }

    public void shutdown() throws IOException, InterruptedException {
        peerIdToNeighbourClientMapping.values().forEach(Client::shutdown);
        taskTimer.cancel();
        fileDownloader.closeFile();
        P2PLogger.getLogger().log(Level.INFO, "Peer [" + peerInfo.getPeerId() + "] has downloaded the complete file.");
        Thread.sleep(Constants.PROCESS_STALL_INTERVAL);
        System.exit(Constants.PROCESS_EXIT_CODE);
    }

    public void addPeerInterest(String peerID, boolean interested) {
        synchronized (peersInterestedInMe) {
            if (interested)
                peersInterestedInMe.add(peerID);
            else
                peersInterestedInMe.remove(peerID);
        }
    }

    public void addPeersInterestedInMe(String peerID) {
        synchronized (peersInterestedInMe) {
            peersInterestedInMe.add(peerID);
        }
    }

    public void removeFromPeersInterestedInMe(String peerID) {
        synchronized (peersInterestedInMe) {
            peersInterestedInMe.remove(peerID);
        }
    }

    public void setPreferredNeighbours() {
        List<Client> peersToSendData = getPeersToSendData();

        if (peersToSendData.isEmpty())
            return;

        peersToSendData.sort((c1, c2) -> {
            if (c1.getDownloadRate() < c2.getDownloadRate()) {
                return 1;
            } else if (c1.getDownloadRate() == c2.getDownloadRate()) {
                return 0;
            }
            return -1;
        });

        List<Client> newPreferredClients =
                new ArrayList<>(peersToSendData.subList(0,
                        Math.min(peersToSendData.size(), commonConfig.getNumberOfPreferredNeighbors())));

        newPreferredClients.forEach(client -> {
            if (newPreferredClients.contains(client) && client != previouslyUnchokedClient) {
                client.unchokeNeighbor();
            }
        });

        neighboursPreferred.forEach(client -> {
            if (!newPreferredClients.contains(client)) {
                client.chokeNeighbor();
            }
        });

        neighboursPreferred = newPreferredClients;
        displayPreferredClients();
    }

    private void displayPreferredClients() {
        List<String> ids = neighboursPreferred.stream()
                .map(client -> client.getNeighborPeerInfo().getPeerId()).collect(Collectors.toList());

        P2PLogger.getLogger().log(Level.INFO, String.format("Peer [%s] has the preferred neighbors [%s]",
                peerInfo.getPeerId(), String.join(", ", ids)));
    }


    public void unchokeNewNeighbour() {
        List<Client> eligiblePeers = getPeersToSendData();
        eligiblePeers.removeAll(neighboursPreferred);

        if (eligiblePeers.isEmpty()) {
            return;
        }

        if (previouslyUnchokedClient != null) {
            previouslyUnchokedClient.chokeNeighbor();
        }

        previouslyUnchokedClient = eligiblePeers.get(new Random().nextInt(eligiblePeers.size()));
        previouslyUnchokedClient.unchokeNeighbor();

        P2PLogger.getLogger().log(Level.INFO, "Peer [" + peerInfo.getPeerId() + "] has the optimistically unchoked neighbor ["
                + previouslyUnchokedClient.getPeer().getPeerInfo().getPeerId() + "].");
    }

    public ConcurrentHashMap<String, Client> getPeerIdToNeighbourClientMapping() {
        return peerIdToNeighbourClientMapping;
    }

    public int getNumberOfPiecesToBeDownloaded() {
        return numberOfPiecesToBeDownloaded;
    }

    public FileDownloader getFileDownloader() {
        return fileDownloader;
    }
}
