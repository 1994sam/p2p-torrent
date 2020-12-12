package org.networks.java.model;

import java.util.BitSet;

public class PeerInfo {

	private String peerId;
	private String hostName;
	private int portNumber;
	private boolean filePresent;
	private BitSet pieceIndexes;

	public String getPeerId() {
		return peerId;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int port) {
		this.portNumber = port;
	}

	public BitSet getPieceIndexes() {
		return pieceIndexes;
	}

	public void setPieceIndexes(BitSet pieceIndexes) {
		this.pieceIndexes = pieceIndexes;
	}

	public Boolean isFilePresent() {
		return filePresent;
	}

	public void setFilePresent(Boolean filePresent) {
		this.filePresent = filePresent;
	}

	public PeerInfo() {
		this(null);
	}

	public PeerInfo(String peerID) {
		this(peerID, null, -1, false);
	}

	public PeerInfo(String peerId, String hostName, Integer port, Boolean filePresent) {
		this.peerId = peerId;
		this.hostName = hostName;
		this.portNumber = port;
		this.filePresent = filePresent;
		pieceIndexes = new BitSet();
	}

	@Override
	public String toString() {
		return "PeerInfo{" +
			"peerID='" + peerId + '\'' +
			", hostName='" + hostName + '\'' +
			", portNumber=" + portNumber +
			", filePresent=" + filePresent +
			'}';
	}
}
