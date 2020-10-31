package org.networks.java.helper;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileHandler {
	private final String filePath;
	private final boolean readOnly;

	private final RandomAccessFile randomAccessFile;
	private final ReentrantReadWriteLock lock;
	private final Map<Integer, byte[]> pieces;

	private final CommonConfig commonConfig;

	public FileHandler(String filePath, boolean readOnly, CommonConfig commonConfig) throws IOException {
		this.filePath = filePath;
		this.readOnly = readOnly;
		this.commonConfig = commonConfig;

		lock = new ReentrantReadWriteLock();
		pieces = new HashMap<>();

		if (readOnly) {
			randomAccessFile = readFile();
		} else {
			randomAccessFile = new RandomAccessFile(filePath, "rw");
			randomAccessFile.setLength(commonConfig.getFileSize());
		}
	}

	private RandomAccessFile readFile() throws IOException {
		RandomAccessFile file = new RandomAccessFile(filePath, "r");
		int fileSize = commonConfig.getFileSize();
		int pieceSize = commonConfig.getPieceSize();
		int numberOfPieces = (int) Math.ceil(fileSize / (float) pieceSize);
		for (int i = 0; i < numberOfPieces; i++) {
			int remainingSize = Math.min(fileSize - i * pieceSize, pieceSize);
			byte[] piece = new byte[remainingSize];
			file.seek(i * pieceSize);
			file.readFully(piece);
			pieces.put(i, piece);
		}
		return file;
	}

	public void addFilePiece(byte[] piece, int pieceIndex) {
		pieces.put(pieceIndex, piece);
	}

	public byte[] getFilePiece(int pieceIndex) {
		return pieces.get(pieceIndex);
	}

	public void closeFile() throws IOException {
		if (!readOnly) {
			lock.writeLock().lock();
			writeFileToDisk();
			lock.writeLock().unlock();
		}
	}

	private void writeFileToDisk() throws IOException {
		int fileSize = commonConfig.getFileSize();
		int pieceSize = commonConfig.getPieceSize();
		int numberOfPieces = (int) Math.ceil(fileSize / (float) pieceSize);
		for (int i = 0; i < numberOfPieces; i++) {
			randomAccessFile.seek(i * pieceSize);
			randomAccessFile.write(pieces.get(i));
		}
	}
}
