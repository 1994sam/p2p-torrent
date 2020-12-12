package org.networks.java;

import org.networks.java.helper.PeerConfig;
import org.networks.java.model.PeerInfo;
import org.networks.java.service.Peer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeerProcess {

    public static void main(String[] args) throws IOException {
        assert args.length == 1;
        PeerInfo peerInfo = null;
        List<PeerInfo> neighbours = new ArrayList<>();
        for (PeerInfo info : new PeerConfig().getPeerInfo()) {
            if (info.getPeerId().equals(args[0])) {
                peerInfo = info;
            }
            if (peerInfo == null) {
                neighbours.add(info);
            }
        }
        assert peerInfo != null;
        new Peer(peerInfo, neighbours).start();
    }
}
