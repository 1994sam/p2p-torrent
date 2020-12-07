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
        List<Client> completedPeers = peer.getPeersFinished();
        if (completedPeers.size() == peer.getNeighbors().size()
                && peer.getPieceTracker().get(peer.peerInfo.getPeerId()).size() == peer.totalPieces) {
            try {
                peer.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            peer.getTaskTimer().cancel();
        }
    }
}
