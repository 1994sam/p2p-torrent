package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.MessageStream;
import org.networks.java.helper.Const;
import org.networks.java.model.PeerInfo;

import java.net.Socket;
import java.util.logging.Level;

public class Server implements Runnable {

	private boolean connected;
	private String neighborPeerId;
	private String msg;
	private String logMsg;
	final private CommonConfig commonConfig;
	private MessageStream msgStream;
	final private PeerInfo peerInfo;
	private Socket socketCon;


	public Server() {
		this(null, null, null);
	}

	public Server(final CommonConfig commonConfig,final PeerInfo peerInfo, Socket socketCon) {
		connected = false;
		neighborPeerId = "";
		msg = "";
		logMsg = "";
		this.commonConfig = commonConfig;
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
		neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
		logMsg = "Peer " + peerInfo.getPeerId() + " is connected from " + neighborPeerId + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		msgStream.sendHandshakeMsg(peerInfo.getPeerId());
		connected = true;
//		processBitField();
	}

	private void processBitField() {
		if(peerInfo.isFilePresent()) {
			msgStream.sendBitFieldMsg(306);
		}
	}

	private void processMessage() {
		msgStream.readMsg();
	}
}
