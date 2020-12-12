package org.networks.java.model;


import org.networks.java.helper.Constants;

public class HandshakeMessage {

    private final byte[] zeroBits;
    private final String peerID;

    public HandshakeMessage(String peerID) {
        this.zeroBits = new byte[10];
        this.peerID = peerID;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(Constants.HANDSHAKE_HEADER);
        builder.append(new String(zeroBits));
        builder.append(peerID);
        return builder.toString();
    }
}