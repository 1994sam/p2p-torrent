package org.networks.java.tasks;

import org.networks.java.helper.CommonConfig;
import org.networks.java.service.Peer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

public class GeneratePreferredNeighbors extends TimerTask {

    private final Peer peer;

    public GeneratePreferredNeighbors(final Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        if (peer.getPeerIdToNeighbourClientMapping().size() > 0) {
            peer.determinePreferredNeighbors();
        }
    }
}
