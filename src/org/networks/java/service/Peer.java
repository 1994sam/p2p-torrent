package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.model.PeerInfo;

import java.util.List;
import java.util.logging.Level;

public class Peer {

	private String peerId;
	final private CommonConfig commonConfig;
	final private PeerInfo peerInfo;
	private List<PeerInfo> neighborPeers;

	public Peer() {
		this(null, null, null);
	}

	public Peer(final CommonConfig commonConfig, final PeerInfo peerInfo, List<PeerInfo> neighborPeers) {
		this.commonConfig = commonConfig;
		this.peerInfo = peerInfo;
		this.neighborPeers = neighborPeers;
		P2PLogger.setLogger(peerInfo.getPeerId());
	}

	public void run() {
		P2PLogger.getLogger().log(Level.INFO, "Starting peer process");
		new Thread(new Client(commonConfig, peerInfo, neighborPeers)).start();
		new Thread(new Listener(commonConfig, peerInfo)).start();
	}
}
