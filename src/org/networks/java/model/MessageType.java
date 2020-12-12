package org.networks.java.model;

public enum MessageType {
	CHOKE,
	UNCHOKE,
	INTERESTED,
	NOT_INTERESTED,
	HAVE,
	BITFIELD,
	REQUEST,
	PIECE;

	public static MessageType getMsgType(int msgTypeIndex) {
		return MessageType.values()[msgTypeIndex];
	}
}
