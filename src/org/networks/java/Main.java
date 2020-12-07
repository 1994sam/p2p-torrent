package org.networks.java;

import org.networks.java.service.Peer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Peer peer = new Peer(args[0]);
        peer.run();
    }
}
