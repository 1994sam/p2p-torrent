package org.networks.java.model;

import org.networks.java.helper.Constants;

public class Message {

    private Constants.MessageType messageType;
    private byte[] messagePacket;

    public String getMessageType() {
        return messageType.name();
    }

    public void setMessageType(String messageType) {
        this.messageType = Constants.MessageType.valueOf(messageType);
    }

    public byte[] getMessagePacket() {
        return messagePacket;
    }

    public void setMessagePacket(byte[] messagePacket) {
        this.messagePacket = messagePacket;
    }

    public Message(String messageType, byte[] messagePacket) {
        this.messageType = Constants.MessageType.valueOf(messageType);
        this.messagePacket = messagePacket;
    }

    @Override
    public String toString() {
        return "Message Type: " + messageType +
                ", Payload: " + messagePacket.toString();
    }
}
