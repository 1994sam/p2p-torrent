package org.networks.java.helper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileHandler {

	private final String fileDirPath;
	private final String filePath;
	private final boolean readOnly;
	private final String prefix = "TempFile";

	private final RandomAccessFile randomAccessFile;
	private final ReentrantReadWriteLock lock;
	private final Map<Integer, byte[]> pieces;
	private final Map<Integer, String> tempFileNames;

	private final CommonConfig commonConfig;
	private final String peerID;

	public FileHandler(String fileDirPath, boolean readOnly, CommonConfig commonConfig, final String peerID) throws IOException {
		this.fileDirPath = fileDirPath;
		this.filePath = fileDirPath + File.separator + commonConfig.getFileName();
		this.readOnly = readOnly;
		this.commonConfig = commonConfig;

		lock = new ReentrantReadWriteLock();
		pieces = new HashMap<>();
		tempFileNames = new HashMap<>();

		if(!Files.exists(Paths.get(fileDirPath)))
			new File(fileDirPath).mkdir();

		this.peerID = peerID;
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
			file.seek((long) i * pieceSize);
			file.readFully(piece);
			pieces.put(i, piece);
		}
		return file;
	}

	public void addFilePiece(byte[] piece, int pieceIndex, String peerID) throws IOException {
//		String executionPath = System.getProperty("user.dir");
//		String folderPath = executionPath + File.separator + "peer_" + peerID + File.separator;
		File tempFile = File.createTempFile(prefix, null, new File(fileDirPath));
		tempFileNames.put(pieceIndex, tempFile.getAbsolutePath());
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.write(piece);
		fos.flush();
		fos.close();
	}

	public byte[] getFilePiece(int pieceIndex) throws IOException {
		Path path = Paths.get(tempFileNames.get(pieceIndex));
		return Files.readAllBytes(path);
	}

	public void closeFile() throws IOException {
		if (!readOnly) {
			lock.writeLock().lock();
			writeFileToDisk();
			lock.writeLock().unlock();
		}
	}

	public void writeFileToDisk() throws IOException {
//		String executionPath = System.getProperty("user.dir");
//		String folderPath = executionPath + File.separator + "peer_" + peerID + File.separator;

		File dir = new File(fileDirPath);

		PrintWriter pw = new PrintWriter(dir.getAbsolutePath() + File.separator + commonConfig.getFileName());

		String[] fileNames = dir.list();

		if (fileNames != null) {
			for (String fileName : fileNames) {
				File f = new File(dir, fileName);
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = br.readLine();
				while (line != null) {
					pw.println(line);
					line = br.readLine();
				}
				pw.flush();
			}
		}
		System.out.println("Reading from all files in directory " + dir.getName() + " Completed");
	}

}
