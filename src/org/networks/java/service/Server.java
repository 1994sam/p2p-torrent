package org.networks.java.service;

import org.networks.java.helper.Const;
import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class Server implements Runnable {

	private final Peer peer;

	public Server(final Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(peer.peerInfo.getPortNum())) {
			while(true) {
				Socket socket = serverSocket.accept();
				MessageStream msgStream = new MessageStream(socket);
				String neighborPeerId = msgStream.readHandshakeMsg(Const.HANDSHAKE_MSG_LEN);
				if(!peer.getNeighborClientTable().containsKey(neighborPeerId)) {
					PeerInfo neighborPeerInfo = new PeerInfo(neighborPeerId, socket.getInetAddress().getHostName(), socket.getPort(), false);
					if (neighborPeerInfo.isFilePresent())
						neighborPeerInfo.getPieceIndexes().set(0, peer.totalPieces);
					Client target = new Client(peer, neighborPeerInfo, socket, msgStream);
					new Thread(target).start();
					P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " is connected from " + neighborPeerId + ".");
					peer.getNeighborClientTable().put(neighborPeerId, target);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
