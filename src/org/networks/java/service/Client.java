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

	private boolean connected;
	private String msg;
	private String logMsg;
	private MessageStream msgStream;
	private PeerInfo peerInfo;
	private PeerInfo neighborPeer;
	private List<PeerInfo> neighborPeers;

	public Client() {
		this(null, null);
	}

	public Client(PeerInfo peerInfo, List<PeerInfo> neighborPeers) {
		connected = false;
		msg = "";
		logMsg = "";
		this.peerInfo = peerInfo;
		neighborPeer = null;
		this.neighborPeers = neighborPeers;
	}

	@Override
	public void run() {

		neighborPeer = neighborPeers.get(0);

		while(true) {
			try (Socket socketCon = new Socket(neighborPeer.getHostName(), neighborPeer.getPortNum())) {

				P2PLogger.getLogger().log(Level.INFO, "Peer is connected to " + neighborPeer.getHostName() + ":" + neighborPeer.getPortNum());
				msgStream = new MessageStream(socketCon);
				if(!connected)
					processHandshake();
				else
					processMessage();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}

	private void processHandshake() {
		logMsg = "Peer " + peerInfo.getPeerId() + " sending connection request to " + neighborPeer.getPeerId() + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		msg = MessageUtil.createHandshakeMsg(peerInfo.getPeerId());
		msgStream.sendMsg(msg);

		msg = msgStream.readMsg(Const.HANDSHAKE_MSG_LEN);
		String neighborPeerId = MessageUtil.readHandshakeMsg(msg);
		if(!neighborPeerId.isEmpty() && neighborPeer.getPeerId().equals(neighborPeerId) ) {
			logMsg = "Peer " + peerInfo.getPeerId() + " makes a connection to " + neighborPeer.getPeerId() + ".";
			P2PLogger.getLogger().log(Level.INFO, logMsg);
			connected = true;
		}
	}

	private void processMessage() {
		logMsg = "Peer " + peerInfo.getPeerId() + " sending connection confirmation to " + neighborPeer.getPeerId() + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		msgStream.sendMsg("Connection Established");
	}
}
