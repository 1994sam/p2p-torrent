package org.networks.java.service;

import org.networks.java.helper.MessageStream;
import org.networks.java.helper.Const;
import org.networks.java.helper.MessageUtil;
import org.networks.java.model.PeerInfo;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

	private boolean connected;
	private String neighborPeerId;
	private String msg;
	private String logMsg;
	private MessageStream msgStream;
	private Socket socketCon;
	private PeerInfo peerInfo;

	public Server() {
		this(null, null);
	}

	public Server(PeerInfo peerInfo, Socket socketCon) {
		connected = false;
		neighborPeerId = "";
		msg = "";
		logMsg = "";
		this.peerInfo = peerInfo;
		this.socketCon = socketCon;
	}

	@Override
	public void run() {
		logMsg = "Peer " + peerInfo.getPeerId() + " receiving messages on " + peerInfo.getPortNum() + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		msgStream = new MessageStream(socketCon);

		while(true) {
			if(!connected)
				processHandshake();
			else
				processMessage();
		}
	}

	private void processHandshake() {
		msg = msgStream.readMsg(Const.HANDSHAKE_MSG_LEN);
		neighborPeerId = MessageUtil.readHandshakeMsg(msg);
		logMsg = "Peer " + peerInfo.getPeerId() + " is connected from " + neighborPeerId + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		msg = MessageUtil.createHandshakeMsg(peerInfo.getPeerId());
		msgStream.sendMsg(msg);
		connected = true;
	}

	private void processMessage() {

	}
}
