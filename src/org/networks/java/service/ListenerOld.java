package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;

public class ListenerOld implements Runnable {

	private String logMsg;
	private final CommonConfig commonConfig;
	private final PeerInfo peerInfo;
	private Set<PeerInfo> neighborPeers;

	public ListenerOld() {
		this(null, null, null);
	}

	public ListenerOld(final CommonConfig commonConfig, final PeerInfo peerInfo, Set<PeerInfo> neighborPeers) {
		logMsg = "";
		this.commonConfig = commonConfig;
		this.peerInfo = peerInfo;
		this.neighborPeers = neighborPeers;
	}

	@Override
	public void run() {
//		logMsg = "Peer " + peerInfo.getPeerId() + " listening on " + peerInfo.getPortNum() + ".";
//		P2PLogger.getLogger().log(Level.INFO, logMsg);

		try (ServerSocket listener = new ServerSocket(peerInfo.getPortNum())) {

			int clientNum = 1;
			while(true) {
				PeerInfo clientPeer = new PeerInfo("ClientOld-" + clientNum);
				ServerOld server = new ServerOld(commonConfig, peerInfo, clientPeer, listener.accept());
				new Thread(server).start();
//				logMsg = "Peer " + peerInfo.getPeerId() + " connecting to client " + clientNum + ".";
//				P2PLogger.getLogger().log(Level.INFO, logMsg);
				clientNum++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
