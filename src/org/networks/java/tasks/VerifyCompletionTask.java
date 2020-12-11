package org.networks.java.tasks;

import org.networks.java.service.Peer;

import java.io.IOException;
import java.util.TimerTask;

public class VerifyCompletionTask extends TimerTask {

    private final Peer peer;

    public VerifyCompletionTask(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        if (peer.areAllNeighborsCompleted()
                && peer.getPieceTracker().get(peer.peerInfo.getPeerId()).size() == 0) {
            try {
                peer.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
            peer.getTaskTimer().cancel();
        }
    }
}
