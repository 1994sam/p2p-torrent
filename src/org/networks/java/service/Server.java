package org.networks.java.service;

import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class Server implements Runnable {

	Peer peer;

	public Server() {
	}

	public Server(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(peer.getPeerInfo().getPortNum())) {
			while (true) {
				Socket socket = serverSocket.accept();
				MessageStream msgStream = new MessageStream(socket);

				String message = msgStream.getInputStream().readUTF();
				String neighborPeerId = message.substring(28, 32);

				PeerInfo neighbor = new PeerInfo(neighborPeerId, socket.getInetAddress().getHostAddress(), socket.getPort(), false);
				Client client = new Client(peer, neighbor, socket, msgStream);
				peer.addClient(client);

				P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] is connected from Peer [" + neighborPeerId + "].");

				Thread clientThread = new Thread(client);
				clientThread.start();
			}
		} catch (IOException e) {
		}

	}
}
