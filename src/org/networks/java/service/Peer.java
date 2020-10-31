package org.networks.java.service;

import org.networks.java.model.PeerInfo;

import java.util.List;
import java.util.logging.Level;

public class Peer {

	private String peerId;
	private PeerInfo peerInfo;
	private List<PeerInfo> neighborPeers;

	public Peer() {}

	public Peer(PeerInfo peerInfo, List<PeerInfo> neighborPeers) {
		this.peerInfo = peerInfo;
		this.neighborPeers = neighborPeers;
		P2PLogger.setLogger(peerInfo.getPeerId());
	}

	public void run() {
		P2PLogger.getLogger().log(Level.INFO, "Starting peer process");
		new Thread(new Client(peerInfo, neighborPeers)).start();
//		new Thread(new Listener(peerInfo)).start();
	}
}
