package org.networks.java.service;

import org.networks.java.helper.Const;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

public class Listener implements Runnable {

	private PeerInfo peerInfo;

	public Listener() {}

	public Listener(PeerInfo peerInfo) {
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		P2PLogger.getLogger().log(Level.INFO, "Peer is listening on " + peerInfo.getPortNum());

		try {
			ServerSocket listener = new ServerSocket(peerInfo.getPortNum());

			int clientNum = 1;
			while(true) {

				Server server = new Server(peerInfo, listener.accept());
				new Thread(server).start();
				P2PLogger.getLogger().log(Level.INFO, "Client " + clientNum + " is connected.");
				clientNum++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
