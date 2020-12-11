package org.networks.java.helper;

import org.networks.java.model.PeerInfo;
import org.networks.java.service.P2PLogger;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileDownloadManager {


    private final BitSet piecesBitSet;
    private final ReadWriteLock piecesBitSetLock = new ReentrantReadWriteLock();

    private final int totalPieces;

    private final FileHandler fileHandler;

    private final PeerInfo peerInfo;

    public FileDownloadManager(final PeerInfo peerInfo, final CommonConfig commonConfig) throws IOException {
        this.peerInfo = peerInfo;

        totalPieces = (int) Math.ceil((double) commonConfig.getFileSize() / commonConfig.getPieceSize());
        piecesBitSet = new BitSet(totalPieces);
        if (peerInfo.isFilePresent()) {
            piecesBitSet.set(0, totalPieces);
        }

        fileHandler = new FileHandler(commonConfig.getFileName(), peerInfo.isFilePresent(), commonConfig, peerInfo.getPeerId());
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
            if (piecesBitSet.get(pieceIndex))
                return false;

            if (!piecesBitSet.get(pieceIndex)) {
                fileHandler.addFilePiece(data, pieceIndex);
                piecesBitSet.set(pieceIndex);
                if (piecesBitSet.nextClearBit(0) >= totalPieces) {
                    P2PLogger.getLogger().info("Peer " + peerInfo.getPeerId() + " has finished downloading the file.");
                    fileHandler.writeFileToDisk();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            piecesBitSetLock.writeLock().unlock();
        }
        return true;
    }
}
