package org.networks.java.service;

import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

public class Client implements Runnable {

	private boolean connectionEstablished;

	private MessageStream msgStream;
	private Peer peer;
	private PeerInfo neighborPeerInfo;
	private Socket socket;

	public Client(final Peer peer, final PeerInfo neighborPeerInfo) {
		Socket socket = null;
		try {
			socket = new Socket(neighborPeerInfo.getHostName(), neighborPeerInfo.getPortNum());
		} catch (IOException e) {
			e.printStackTrace();
		}
		initializeClient(peer, neighborPeerInfo, socket, new MessageStream(socket),false);
	}

	public Client(final Peer peer, final PeerInfo neighborPeerInfo, final Socket socket, final MessageStream msgStream) {
		initializeClient(peer, neighborPeerInfo, socket, msgStream, true);
	}

	@Override
	public void run() {
		do processHandShake();
		while(!connectionEstablished);
		peer.neighborClientTable.put(neighborPeerInfo.getPeerId(), this);
	}

	private void initializeClient(final Peer peer, final PeerInfo neighborPeerInfo, final Socket socket, final MessageStream msgStream, final boolean connectionEstablished) {
		this.peer = peer;
		this.neighborPeerInfo = neighborPeerInfo;
		this.socket = socket;
		this.msgStream = msgStream;
		this.connectionEstablished = connectionEstablished;
	}

	private void processHandShake() {
		if(msgStream.sendHandshakeMsg(peer.peerInfo.getPeerId())) {
			while (!connectionEstablished) {
				String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
				if (!neighborPeerId.isEmpty() && neighborPeerInfo.getPeerId().equals(neighborPeerId)) {
					connectionEstablished = true;
					P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " makes a connection to " + neighborPeerInfo.getPeerId() + ".");
				}
			}
		}
	}
}
