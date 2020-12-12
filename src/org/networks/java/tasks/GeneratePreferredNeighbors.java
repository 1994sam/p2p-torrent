package org.networks.java.tasks;

import org.networks.java.helper.CommonConfig;
import org.networks.java.service.Peer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

public class GeneratePreferredNeighbors extends TimerTask {

    private final Peer peer;
    private final CommonConfig commonConfig;

    public GeneratePreferredNeighbors(Peer peer, CommonConfig commonConfig) {
        this.peer = peer;
        this.commonConfig = commonConfig;
    }

    @Override
    public void run() {
        List<String> interestedPeers = peer.getInterestedPeers();
        if (!interestedPeers.isEmpty()) {
            if (peer.peerInfo.isFilePresent()) {
                peer.setPreferredNeighbors(pickNRandom(interestedPeers, commonConfig.getNumberOfPreferredNeighbors()));
            } else {

            }
        }
    }

    public static List<String> pickNRandom(List<String> peers, int n) {
        List<String> copy = new ArrayList<>(peers);
        Collections.shuffle(copy);
        return n > copy.size() ? copy.subList(0, copy.size()) : copy.subList(0, n);
    }
}
