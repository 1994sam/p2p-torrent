package org.networks.java.service;

import org.networks.java.helper.MessageStream;
import org.networks.java.helper.Const;
import org.networks.java.helper.MessageUtil;
import org.networks.java.model.PeerInfo;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

	private String msg;
	private MessageStream msgStream;
	private Socket socketCon;
	private PeerInfo peerInfo;

	public Server() {}

	public Server(PeerInfo peerInfo, Socket socketCon) {
		this.peerInfo = peerInfo;
		this.socketCon = socketCon;
	}

	@Override
	public void run() {
		P2PLogger.getLogger().log(Level.INFO, "Peer is receiving messages on " + peerInfo.getPortNum());

		msgStream = new MessageStream(socketCon);

		while(true) {
			processHandshake();
		}
	}

	private void processHandshake() {
		msg = msgStream.readMsg(Const.HANDSHAKE_MSG_LEN);
		String neighborPeerId = MessageUtil.readHandshakeMsg(msg);
		P2PLogger.getLogger().log(Level.INFO, "Received connection request and sending acknowledgment");
		msg = MessageUtil.createHandshakeMsg(peerInfo.getPeerId());
		msgStream.sendMsg(msg);
	}
}
