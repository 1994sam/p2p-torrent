package org.networks.java.model;

public class PeerInfo {

	private String peerId;
	private String hostName;
	private Integer portNum;
	private Boolean filePresent;

	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public Integer getPortNum() {
		return portNum;
	}

	public void setPortNum(Integer portNum) {
		this.portNum = portNum;
	}

	public Boolean getFilePresent() {
		return filePresent;
	}

	public void setFilePresent(Boolean filePresent) {
		this.filePresent = filePresent;
	}

	public PeerInfo() {
		this(null);
	}

	public PeerInfo(String peerId) {
		this(peerId, null, -1, false);
	}

	public PeerInfo(String peerId, String hostName, Integer port, Boolean filePresent) {
		this.peerId = peerId;
		this.hostName = hostName;
		this.portNum = port;
		this.filePresent = filePresent;
	}

	@Override
	public String toString() {
		return "PeerInfo{" +
			"peerID='" + peerId + '\'' +
			", hostName='" + hostName + '\'' +
			", portNumber=" + portNum +
			", filePresent=" + filePresent +
			'}';
	}
}
