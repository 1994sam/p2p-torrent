package org.networks.java.tasks;

import org.networks.java.service.Client;
import org.networks.java.service.Peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TimerTask;

public class VerifyCompletionTask extends TimerTask {

    private final Peer peer;

    public VerifyCompletionTask(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        ArrayList<Client> completedPeers = peer.getCompletedPeers();
        if (completedPeers.size() == peer.getPeerIdToNeighbourClientMapping().size()
                && peer.getPieceIndexStore().get(peer.getPeerInfo().getPeerId()).size() == peer.getNumberOfPiecesToBeDownloaded()) {
            try {
                peer.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
