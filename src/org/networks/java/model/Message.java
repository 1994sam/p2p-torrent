package org.networks.java.model;

import java.util.Arrays;

public class Message {

	private MessageType msgType;
	private byte[] msgBody;

	public MessageType getMsgType() {
		return msgType;
	}

	public void setMsgType(MessageType msgType) {
		this.msgType = msgType;
	}

	public byte[] getMsgBody() {
		return msgBody;
	}

	public void setMsgBody(byte[] msgBody) {
		this.msgBody = msgBody;
	}

	public Message(MessageType msgType, byte[] msgBody) {
		this.msgType = msgType;
		this.msgBody = msgBody;
	}

	@Override
	public String toString() {
		return  msgType.toString() + Arrays.toString(msgBody);
	}
}
