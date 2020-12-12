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
        List<Client> runningClients = peer.getNeighbors();
//        List<Client> runningClients = peer.getRunningClients();

        if (!runningClients.isEmpty()) {

            if(peer.previouslyUnchokedNeighbor != null) {
                peer.previouslyUnchokedNeighbor.chokeNeighbor();
            }

            Random random = new Random();
            Client client = runningClients.get(random.nextInt(runningClients.size()));
            if(client != null && !client.neighborPeerInfo.getPeerId().equals(peer.peerInfo.getPeerId())) {
                peer.previouslyUnchokedNeighbor = client;
                client.unchokeNeighbor();
//                P2PLogger.getLogger().log(Level.INFO, "Peer " + peer.peerInfo.getPeerId() + " has the unchoked neighbor " + client.neighborPeerInfo.getPeerId());
            }
        }
    }
}
