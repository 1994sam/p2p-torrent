package org.networks.java.model;

import org.networks.java.helper.MessageType;

import java.util.Arrays;

public class Message {

    private MessageType messageType;
    private byte[] messagePacket;

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = MessageType.valueOf(messageType);
    }

    public byte[] getMessagePacket() {
        return messagePacket;
    }

    public void setMessagePacket(byte[] messagePacket) {
        this.messagePacket = messagePacket;
    }

    public Message(MessageType messageType, byte[] messagePacket) {
        this.messageType = messageType;
        this.messagePacket = messagePacket;
    }

    @Override
    public String toString() {
        return "Message Type: " + messageType +
                ", Payload: " + Arrays.toString(messagePacket);
    }
}
