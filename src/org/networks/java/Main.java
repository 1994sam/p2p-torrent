package org.networks.java;

import org.networks.java.helper.CommonConfig;
import org.networks.java.helper.PeerConfig;
import org.networks.java.model.PeerInfo;
import org.networks.java.service.Peer;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        final CommonConfig commonConfig = new CommonConfig();
        final List<PeerInfo> peers = new PeerConfig().getPeerInfo();

//        Peer peer1 = new Peer(commonConfig, peers.get(0), new ArrayList<PeerInfo>() {{ add(peers.get(1)); }} ); peer1.run();
        Peer peer2 = new Peer(commonConfig, peers.get(1), new ArrayList<PeerInfo>() {{ add(peers.get(0)); }} ); peer2.run();
    }
}
