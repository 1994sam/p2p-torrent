package org.networks.java.helper;

import java.io.*;
import java.util.Properties;

public class CommonConfig {

	private static final String NUMBER_OF_PREFERRED_NEIGHBORS = "NumberOfPreferredNeighbors";
	private static final String UNCHOKING_INTERVAL = "UnchokingInterval";
	private static final String OPTIMISTIC_UNCHOKING_INTERVAL = "OptimisticUnchokingInterval";
	private static final String FILE_NAME = "FileName";
	private static final String FILE_SIZE = "FileSize";
	private static final String PIECE_SIZE = "PieceSize";

	private final Properties properties;

	public int getNumberOfPreferredNeighbors() {
		return Integer.parseInt(properties.getProperty(NUMBER_OF_PREFERRED_NEIGHBORS));
	}

	public int getUnchokingInterval() {
		return Integer.parseInt(properties.getProperty(UNCHOKING_INTERVAL));
	}

	public int getOptimisticUnchokingInterval() {
		return Integer.parseInt(properties.getProperty(OPTIMISTIC_UNCHOKING_INTERVAL));
	}

	public String getFileName() {
		return properties.getProperty(FILE_NAME);
	}

	public int getFileSize() {
		return Integer.parseInt(properties.getProperty(FILE_SIZE));
	}

	public int getPieceSize() {
		return Integer.parseInt(properties.getProperty(PIECE_SIZE));
	}

	public CommonConfig() {
		this(Constants.COMMON_CFG_FILE_NAME);
	}

	public CommonConfig(String fileName) {
		String filePath = System.getProperty(Constants.USER_DIR_PATH) + File.separator + fileName;
		final File cfgFile = new File(filePath);
		properties = new Properties();
		try (final InputStream inStream = new DataInputStream(new FileInputStream(cfgFile))){
			properties.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "CommonConfig{" +
			"NumberOfPreferredNeighbors='" + getNumberOfPreferredNeighbors() + '\'' +
			", UnchokingInterval='" + getUnchokingInterval() + '\'' +
			", OptimisticUnchokingInterval=" + getOptimisticUnchokingInterval() +
			", FileName=" + getFileName() +
			", FileSize=" + getFileSize() +
			", PieceSize=" + getPieceSize()+
			'}';
	}
}
