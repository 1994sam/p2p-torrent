package org.networks.java.helper;

import org.networks.java.model.PeerInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PeerConfig {
	private final String configFileName;

	public PeerConfig() {
		this(Const.PEER_INFO_CFG_FILE_NAME);
	}

	public PeerConfig(final String fileName) {
		configFileName = fileName;
	}

	public List<PeerInfo> getPeerInfo() {
		String filePath = System.getProperty(Const.USER_DIR_PATH) + File.separator + configFileName;
		try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
			return stream.map(line -> {
				String[] values = line.split("\\s+");
				return new PeerInfo(values[0], values[1], Integer.parseInt(values[2]), "1".equals(values[3]));
			}).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.EMPTY_LIST;
	}
}
