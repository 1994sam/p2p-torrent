package org.networks.java.service;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.MessageStream;
import org.networks.java.helper.Const;
import org.networks.java.model.PeerInfo;

import java.net.Socket;
import java.util.logging.Level;

public class ServerOld implements Runnable {

	private boolean connected;
	private String msg;
	private String logMsg;
	final private CommonConfig commonConfig;
	private MessageStream msgStream;
	final private PeerInfo peerInfo;
	private PeerInfo clientPeerInfo;
	private Socket socketCon;


	public ServerOld() {
		this(null, null, null, null);
	}

	public ServerOld(final CommonConfig commonConfig, final PeerInfo peerInfo, final PeerInfo clientPeerInfo, Socket socketCon) {
		connected = false;
		msg = "";
		logMsg = "";
		this.commonConfig = commonConfig;
		this.peerInfo = peerInfo;
		this.clientPeerInfo = clientPeerInfo;
		this.socketCon = socketCon;
	}

	@Override
	public void run() {
		msgStream = new MessageStream(socketCon);

		while(true) {
			if(!connected)
				processHandshake();
			else
				processMessage();
		}
	}

	private void processHandshake() {
		String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
		if(!neighborPeerId.isEmpty() && msgStream.sendHandshakeMsg(peerInfo.getPeerId())) {
			clientPeerInfo.setPeerId(neighborPeerId);
			connected = true;
			logMsg = "Peer " + peerInfo.getPeerId() + " is connected from " + neighborPeerId + ".";
			P2PLogger.getLogger().log(Level.INFO, logMsg);
		}
		processBitField();
	}

	private void processBitField() {
		if(peerInfo.getPieceIndexes().size() > 0) {
			while(!msgStream.sendBitFieldMsg(peerInfo.getPieceIndexes()));
		}
	}

	private void processMessage() {
		msgStream.readMsg(clientPeerInfo);
	}
}
