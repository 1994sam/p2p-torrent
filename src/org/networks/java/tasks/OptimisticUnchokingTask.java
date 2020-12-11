package org.networks.java.tasks;

import org.networks.java.service.Client;
import org.networks.java.service.P2PLogger;
import org.networks.java.service.Peer;

import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.logging.Level;

public class OptimisticUnchokingTask extends TimerTask {

    private final Peer peer;

    public OptimisticUnchokingTask(Peer peer) {
        this.peer = peer;
    }


    @Override
    public void run() {
        List<Client> neighbors = peer.getNeighbors();
        List<Client> peersFinished = peer.getPeersFinished();
        neighbors.removeAll(peersFinished);
        if (!neighbors.isEmpty()) {
            Random random = new Random();
            Client client = neighbors.get(0);
            client.unchokeNeighbor();
            if (client.getPeer().peerInfo.getPeerId().equals(peer.peerInfo.getPeerId())) {
                client = neighbors.get(neighbors.size() - 1);
            }
            if (!client.getPeer().peerInfo.getPeerId().equals(peer.peerInfo.getPeerId())) {
                client.unchokeNeighbor();
                P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId()
                        + " has the unchoked neighbor " + client.getPeer().peerInfo.getPeerId());
            }
        }
    }
}
