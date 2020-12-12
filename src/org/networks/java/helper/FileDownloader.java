package org.networks.java.helper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileDownloader {

    private final String filePath;
    private final boolean isReadOnlyFile;
    private final ReentrantReadWriteLock lock;

    private final RandomAccessFile file;

    private final Map<Integer, byte[]> filePieces;

    private final CommonConfig commonConfig;

    public FileDownloader(final String filePath, final boolean isReadOnlyFile,
                          final CommonConfig commonConfig, final String peerID) throws IOException {
        this.filePath = Constants.FILE_DIR_PREFIX_PATH + peerID + File.separator + filePath;
        this.isReadOnlyFile = isReadOnlyFile;
        this.commonConfig = commonConfig;
        lock = new ReentrantReadWriteLock();
        filePieces = new HashMap<>();
        file = getFile(this.filePath, isReadOnlyFile, commonConfig);
    }

    private RandomAccessFile getFile(final String filePath, final boolean readOnly,
                                     final CommonConfig commonConfig) throws IOException {
        final RandomAccessFile randomAccessFile;
        if (readOnly) {
            randomAccessFile = readFile();
        } else {
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            randomAccessFile.setLength(commonConfig.getFileSize());
        }
        return randomAccessFile;
    }

    private RandomAccessFile readFile() throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath, "r");
        int numberOfPieces = (int) Math.ceil(commonConfig.getFileSize() / (float) commonConfig.getPieceSize());
        for (int i = 0; i < numberOfPieces; i++) {
            int remainingSize = Math.min(commonConfig.getFileSize() - i * commonConfig.getPieceSize(), commonConfig.getPieceSize());
            byte[] piece = new byte[remainingSize];
            file.seek((long) i * commonConfig.getPieceSize());
            file.readFully(piece);
            filePieces.put(i, piece);
        }
        return file;
    }

    public void closeFile() throws IOException {
        lock.writeLock().lock();
        if (!isReadOnlyFile) {
            pushFileToDisk();
        }
        lock.writeLock().unlock();
    }

    private void pushFileToDisk() throws IOException {
        int i = 0;
        for (byte[] value : filePieces.values()) {
            file.seek((long) (i++) * commonConfig.getPieceSize());
            file.write(value);
        }
    }

    public void addFilePiece(byte[] piece, int pieceIndex) {
        filePieces.put(pieceIndex, piece);
    }

    public byte[] getFilePiece(int pieceIndex) {
        return filePieces.get(pieceIndex);
    }


}
