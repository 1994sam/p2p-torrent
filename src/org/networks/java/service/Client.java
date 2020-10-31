package org.networks.java.service;

import org.networks.java.helper.MessageStream;
import org.networks.java.helper.Const;
import org.networks.java.helper.MessageUtil;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable {

	private String msg;
	private MessageStream msgStream;
	private PeerInfo peerInfo;
	private PeerInfo neighborPeer;
	private List<PeerInfo> neighborPeers;

	public Client() {}

	public Client(PeerInfo peerInfo, List<PeerInfo> neighborPeers) {
		this.peerInfo = peerInfo;
		this.neighborPeers = neighborPeers;
	}

	@Override
	public void run() {

		neighborPeer = neighborPeers.get(0);

		while(true) {
			try (Socket socketCon = new Socket(neighborPeer.getHostName(), neighborPeer.getPortNum())) {

				P2PLogger.getLogger().log(Level.INFO, "Peer is connected to " + neighborPeer.getHostName() + ":" + neighborPeer.getPortNum());
				msgStream = new MessageStream(socketCon);
				processHandshake();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}

	private void processHandshake() {
		P2PLogger.getLogger().log(Level.INFO, "Sending connection request");
		msg = MessageUtil.createHandshakeMsg(peerInfo.getPeerId());
		msgStream.sendMsg(msg);

		msg = msgStream.readMsg(Const.HANDSHAKE_MSG_LEN);
		String neighborPeerId = MessageUtil.readHandshakeMsg(msg);
		if(!neighborPeerId.isEmpty() && neighborPeer.getPeerId().equals(neighborPeerId) ) {
			P2PLogger.getLogger().log(Level.INFO, "Received acknowledgment and sending confirmation");
			msgStream.sendMsg("Connection Established");
		}
	}
}
