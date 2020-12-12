package org.networks.java.tasks;

import org.networks.java.service.Client;
import org.networks.java.service.Peer;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

public class VerifyCompletionTask extends TimerTask {

	private final Peer peer;

	public VerifyCompletionTask(Peer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		List<Client> completedPeers = peer.getCompletedPeers();
		if (completedPeers.size() == peer.getPeerIdToNeighbourClientMapping().size()
			&& peer.getPieceIndexStore().get(peer.getPeerInfo().getPeerId()).size() == peer.getNumberOfPiecesToBeDownloaded()) {
			try {
				peer.shutdown();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
