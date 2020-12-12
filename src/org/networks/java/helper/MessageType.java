package org.networks.java.helper;

public enum MessageType {
    CHOKE("CHOKE"),
    UNCHOKE("UNCHOKE"),
    INTERESTED("INTERESTED"),
    NOT_INTERESTED("NOT_INTERESTED"),
    HAVE("HAVE"),
    BITFIELD("BITFIELD"),
    REQUEST("REQUEST"),
    PIECE("PIECE");

    private String name;

    public static MessageType getMessageValue(int typeIdx) {
        return MessageType.values()[typeIdx];
    }

    MessageType(String name) {
        this.name = name;
    }
}