package org.networks.java.model;

public class Message {

	private String msgType;
	private byte[] msgPacket;

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public byte[] getMsgPacket() {
		return msgPacket;
	}

	public void setMsgPacket(byte[] msgPacket) {
		this.msgPacket = msgPacket;
	}

	public Message(String msgType, byte[] msgPacket) {
		this.msgType = msgType;
		this.msgPacket = msgPacket;
	}

	@Override
	public String toString() {
		return "Message Type: " + msgType +
			", Payload: " + msgPacket.toString();
	}
}
