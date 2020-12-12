package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.FileDownloader;
import org.networks.java.model.PeerInfo;
import org.networks.java.tasks.OptimisticUnchokingTask;
import org.networks.java.tasks.VerifyCompletionTask;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class Peer {

    private final PeerInfo info;
    private int numberOfPiecesToBeDownloaded;
    private final Logger logger;
    private final ReadWriteLock lock;

    private final ConcurrentHashMap<String, BitSet> neighbourPieceIndices = new ConcurrentHashMap<>();
    private BitSet piecesIndex;
    private final ConcurrentHashMap<String, HashSet<Integer>> pieceIndexStore;

    private final ConcurrentHashMap<String, Client> peerIdToNeighbourClientMapping;
    private final List<Client> neighboursPreferred;
    private final List<String> peersInterestedInMe;

    private Client previouslyUnchokedClient;

    private final FileDownloader fileDownloader;

    private final Timer taskTimer = new Timer(true);

    private final CommonConfig commonConfig = new CommonConfig();

    public PeerInfo getInfo() {
        return info;
    }

    // Peer initializes a peer with the required info.
    public Peer(PeerInfo peerInfo) throws IOException {
        info = peerInfo;
        logger = P2PLogger.getLogger(peerInfo.getPeerID());
        peerIdToNeighbourClientMapping = new ConcurrentHashMap<>();
        neighboursPreferred = new ArrayList<>();
        peersInterestedInMe = Collections.synchronizedList(new ArrayList<>());
        pieceIndexStore = new ConcurrentHashMap<>();
        pieceIndexStore.put(peerInfo.getPeerID(), new HashSet<>());

        initializeTracker(peerInfo);

        fileDownloader = new FileDownloader(commonConfig.getFileName(), peerInfo.isFilePresent(), commonConfig);
        lock = new ReentrantReadWriteLock();
    }

    private int initializeTracker(PeerInfo peerInfo) {
        numberOfPiecesToBeDownloaded = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());
        piecesIndex = new BitSet(numberOfPiecesToBeDownloaded);
        if (peerInfo.isFilePresent()) {
            piecesIndex.set(0, numberOfPiecesToBeDownloaded);
            for (int i = 0; i < numberOfPiecesToBeDownloaded; i++)
                pieceIndexStore.get(peerInfo.getPeerID()).add(i);
        }
        return numberOfPiecesToBeDownloaded;
    }

    public int getPort() {
        return info.getPortNumber();
    }

    public String getID() {
        return info.getPeerID();
    }

    public boolean hasOnePiece() {
        return pieceIndexStore.get(getID()).size() > 0;
    }

    // addClient adds the client to the list of neighbors maintained by the peer.
    public void addClient(Client client) {
        neighbourPieceIndices.put(client.getID(), new BitSet(piecesIndex.size()));
        pieceIndexStore.put(client.getID(), new HashSet<Integer>());
        peerIdToNeighbourClientMapping.put(client.getID(), client);
    }

    // start does the following:
    // 1. performs a handshake with the list of input peers.
    // 2. starts a server to listen for incoming connections.
    public void start(List<PeerInfo> peers) {

        for (int i = 0; i < peers.size(); i++) {
            Client client = new Client(this, peers.get(i), logger);
            Thread clientThread = new Thread(client);
            clientThread.start();
        }
        // server listens for incoming connection requests.
        new Server(this, logger).start();

    }

    private void scheduleTasks() {
        taskTimer.schedule(new VerifyCompletionTask(this), 10000, 5000);
        taskTimer.schedule(preferredNeighborTask(), 0, * 1000);
        taskTimer.schedule(new OptimisticUnchokingTask(this), 0, Configs.Common.OptimisticUnchokingInterval * 1000);
    }

    // completionCheckTask runs the completion checker to
    // determine if all the neighbors have received the file.
    // If this condition is met, the peer shuts itself down.
    private TimerTask completionCheckTask() {
        return new TimerTask() {
            public void run() {
                ArrayList<Client> completedPeers = getCompletedPeers();
                if (completedPeers.size() == peerIdToNeighbourClientMapping.size() && pieceIndexStore.get(getID()).size() == numberOfPiecesToBeDownloaded) {
                    shutdown();
                    taskTimer.cancel();
                }
            }
        };
    }

    // preferredNeighborTask is used to recalculate the
    // set of preferred neighbors for the peer.
    private TimerTask preferredNeighborTask() {
        return new TimerTask() {
            public void run() {
                if (peerIdToNeighbourClientMapping.size() > 0) {
                    determinePreferredNeighbors();
                }
            }
        };
    }

    // optimisticallyUnchokedNeighborTask is used to recalculate
    // the optimistically unchoked neighbor for the peer.
    private TimerTask optimisticallyUnchokedNeighborTask() {
        return new TimerTask() {
            public void run() {
                if (peerIdToNeighbourClientMapping.size() > 0) {
                    determineOptimisticallyUnchokedNeighbor();
                }
            }
        };
    }

    // determinePreferredNeighbors calculates and sets the
    // preferred neighbors based on the download rate from
    // the peers.
    private void determinePreferredNeighbors() {
        // get eligible peers by removing the completed peers from
        // the list of all peers.
        ArrayList<Client> eligiblePeers = getPeersEligibleForUpload();
        if (previouslyUnchokedClient != null) {
            eligiblePeers.remove(previouslyUnchokedClient);
        }

        if (eligiblePeers.isEmpty())
            return;

        // sort the peers in decreasing order of the download rate.
        Collections.sort(eligiblePeers, new DownloadRateComparator());

        // pick the first k elements from the list where k
        // is the number of preferred neighbors.
        ArrayList<Client> newPreferredClients =
                new ArrayList<Client>(eligiblePeers.subList(0,
                        Math.min(eligiblePeers.size(), Configs.Common.NumberOfPreferredNeighbors)));

        // unchoke all neighbors that have been picked in this round
        // and aren't currently choked.
        for (int i = 0; i < newPreferredClients.size(); i++) {
            Client pc = newPreferredClients.get(i);
            if (!neighboursPreferred.contains(pc) && pc != previouslyUnchokedClient) {
                newPreferredClients.get(i).unchokeNeighbor();
            }
        }

        // choke all neighbors that are not in the new list but are unchoked.
        for (int i = 0; i < neighboursPreferred.size(); i++) {
            if (!newPreferredClients.contains(neighboursPreferred.get(i))) {
                neighboursPreferred.get(i).chokeNeighbor();
            }
        }

        // update preferred neighbors.
        neighboursPreferred = newPreferredClients;
        logger.info("Peer " + getID() + " has the preferred neighbors " + getIDList(neighboursPreferred));
    }

    private String getIDList(ArrayList<Client> clients) {
        String result = clients.get(0).getID();
        for (int i = 1; i < clients.size(); i++) {
            result += ", " + clients.get(i).getID();
        }
        return result;
    }

    // determineOptimisticallyUnchokedNeighbor calculates and sets the
    // optimistically unchoked neighbor for the peer via random
    // selection.
    private void determineOptimisticallyUnchokedNeighbor() {
        ArrayList<Client> eligiblePeers = getPeersEligibleForUpload();
        eligiblePeers.removeAll(neighboursPreferred);

        if (eligiblePeers.size() == 0) {
            return;
        }

        if (previouslyUnchokedClient != null) {
            previouslyUnchokedClient.chokeNeighbor();
        }

        Random rand = new Random();
        previouslyUnchokedClient = eligiblePeers.get(rand.nextInt(eligiblePeers.size()));
        previouslyUnchokedClient.unchokeNeighbor();

        logger.info("Peer " + getID() + " has the optimistically unchoked neighbor " + previouslyUnchokedClient.getID());
    }

    // getPeersEligibleForUpload returns the list of peers that
    // are eligible for receiving a piece.
    private ArrayList<Client> getPeersEligibleForUpload() {
        ArrayList<Client> completedPeers = getCompletedPeers();
        ArrayList<Client> candidatePeers = new ArrayList<Client>(peerIdToNeighbourClientMapping.values());
        candidatePeers.removeAll(completedPeers);
        return candidatePeers;
    }

    // getCompletedPeers returns list of peers that have
    // completed the file download in the p2p network.
    private ArrayList<Client> getCompletedPeers() {
        lock.readLock().lock();
        ArrayList<Client> completedPeers = new ArrayList<Client>();
        Iterator<Entry<String, HashSet<Integer>>> pieceIterator = pieceIndexStore.entrySet().iterator();
        while (pieceIterator.hasNext()) {
            Map.Entry<String, HashSet<Integer>> peer = (Map.Entry<String, HashSet<Integer>>) pieceIterator.next();
            if (peer.getKey() == getID())
                continue;

            if (peer.getValue().size() == numberOfPiecesToBeDownloaded)
                completedPeers.add(peerIdToNeighbourClientMapping.get(peer.getKey()));
        }
        lock.readLock().unlock();
        return completedPeers;
    }

    // updateNeighborBitField updates the bitfield info for a neighboring peer.
    // This operation is thread safe.
    public void updateNeighborPieceIndex(String peerID, Integer pieceIndex) {
        try {
            lock.writeLock().lock();
            BitSet nBitSet = neighbourPieceIndices.get(peerID);
            nBitSet.set(pieceIndex);
            neighbourPieceIndices.put(peerID, nBitSet);
            pieceIndexStore.get(peerID).add(pieceIndex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // setNeighborBitField sets the bitField for the given peer id in
    // the neighbor bit field map.
    public void setNeighborBitField(String peerID, byte[] bitField) {
        try {
            lock.writeLock().lock();
            BitSet bitSet = getBitSet(bitField);
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

    // hasPiece returns true if the peer has a given piece.
    public boolean hasPiece(Integer pieceIndex) {
        try {
            lock.readLock().lock();
            return pieceIndexStore.get(getID()).contains(pieceIndex);
        } finally {
            lock.readLock().unlock();
        }
    }

    // getBitField returns the bit field for the peer.
    public byte[] getBitField() {
        lock.readLock().lock();
        byte[] bytes = new byte[(piecesIndex.size() + 7) / 8];
        try {
            for (int i = 0; i < piecesIndex.size(); i++) {
                if (piecesIndex.get(i)) {
                    bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return bytes;
    }

    // Returns a bitset containing the values in bytes.
    private BitSet getBitSet(byte[] bytes) {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    // getPieceRequestIndex returns the ID of the piece that
    // needs to be requested from a given peer.
    public int getPieceRequestIndex(String peerID) {
        try {
            lock.readLock().lock();
            ArrayList<Integer> candidatePieces = new ArrayList<Integer>(pieceIndexStore.get(peerID));
            candidatePieces.removeAll(new ArrayList<Integer>(pieceIndexStore.get(getID())));
            if (!candidatePieces.isEmpty()) {
                return candidatePieces.get(new Random().nextInt(candidatePieces.size()));
            }
        } finally {
            lock.readLock().unlock();
        }
        return -1;
    }

    // addPiece saves a given piece for the local peer.
    public boolean addPiece(Integer pieceIndex, byte[] data) throws IOException {
        lock.writeLock().lock();
        try {
            if (pieceIndexStore.get(getID()).contains(pieceIndex))
                return false;

            if (!piecesIndex.get(pieceIndex)) {
                fileDownloader.addPiece(pieceIndex, data);
                piecesIndex.set(pieceIndex);
                pieceIndexStore.get(getID()).add(pieceIndex);
                if (piecesIndex.nextClearBit(0) >= numberOfPiecesToBeDownloaded) {
                    logger.info("Peer " + getID() + " has downloaded the complete file.");
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

        Iterator<Entry<String, Client>> neighborIterator = peerIdToNeighbourClientMapping.entrySet().iterator();
        while (neighborIterator.hasNext()) {
            Map.Entry<String, Client> pair = (Map.Entry<String, Client>) neighborIterator.next();
            pair.getValue().sendHave(pieceIndex);
        }
        return true;
    }

    // getPiece returns the requested piece from the local peer.
    public byte[] getPiece(Integer pieceIndex) throws IOException {
        byte[] piece = null;
        lock.readLock().lock();
        try {
            if (piecesIndex.get(pieceIndex)) {
                piece = fileDownloader.getPiece(pieceIndex);
            }
        } finally {
            lock.readLock().unlock();
        }
        return piece;
    }

    // shutdown shuts the peer down gracefully.
    private void shutdown() {
        Iterator<Entry<String, Client>> neighborIterator = peerIdToNeighbourClientMapping.entrySet().iterator();
        while (neighborIterator.hasNext()) {
            Map.Entry<String, Client> pair = (Map.Entry<String, Client>) neighborIterator.next();
            pair.getValue().shutdown();
        }
        fileDownloader.close();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.exit(0);
    }

    // addPeerInterest keeps track of the interest from peers.
    public void addPeerInterest(String peerID, boolean interested) {
        synchronized (peersInterestedInMe) {
            if (interested)
                peersInterestedInMe.add(peerID);
            else
                peersInterestedInMe.remove(peerID);
        }
    }
}

// DownloadRateComparator sorts the clients in descending
// order of their download rates.
class DownloadRateComparator implements Comparator<Client> {
    @Override
    public int compare(Client c1, Client c2) {
        if (c1.getDownloadRate() < c2.getDownloadRate()) {
            return 1;
        } else if (c1.getDownloadRate() == c2.getDownloadRate()) {
            return 0;
        }
        return -1;
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
            curPeerInfo.setMissingPieces(pieceTracker.get(curPeerInfo.getPeerId()).size());

        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public void updatePiece(int pieceIndex, byte[] piece) {
        bitLock.writeLock().lock();

        try {
            pieces.put(pieceIndex, piece);
            peerInfo.setMissingPieces(peerInfo.getMissingPieces() - 1);
            fileHandler.getPieces().put(pieceIndex, piece);
            checkFileStatus(peerInfo);

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
            checkFileStatus(peerInfo);
            for (PeerInfo neighborPeerInfo : peerInfoTable.values())
                checkFileStatus(neighborPeerInfo);
        } finally {
            bitLock.writeLock().unlock();
        }
    }

    public void checkFileStatus(PeerInfo peerInfo) {
        if (shutdown)
            return;

        if (!peerInfo.isFilePresent())
            peerInfo.setFilePresent(peerInfo.getMissingPieces() == 0);

        if (peerInfo.isFilePresent()) {
            shutdown = true;
            try {
                fileHandler.writeFileToDisk();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
