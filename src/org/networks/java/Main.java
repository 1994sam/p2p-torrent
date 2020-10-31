package org.networks.java;

import org.networks.java.model.PeerInfo;
import org.networks.java.service.Peer;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<PeerInfo> peers = new ArrayList<>();
        peers.add(new PeerInfo("1001", "localhost", 6008, 1 == 1));
        peers.add(new PeerInfo("1002", "localhost", 6009, 0 == 1));

//        Peer peer1 = new Peer(peers.get(0), new ArrayList<PeerInfo>() {{ add(peers.get(1)); }} ); peer1.run();
        Peer peer2 = new Peer(peers.get(1), new ArrayList<PeerInfo>() {{ add(peers.get(0)); }} ); peer2.run();
    }
}
