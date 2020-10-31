package org.networks.java.helper;

import org.networks.java.service.P2PLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageUtil {

	public static String createHandshakeMsg(String peerId) {
		StringBuilder sb = new StringBuilder();
		sb.append(Const.HANDSHAKE_HEADER);
		for(int i = 0; i < Const.HANDSHAKE_ZERO_BITS_LEN; i++)
			sb.append("0");
		sb.append(peerId);
		P2PLogger.getLogger().log(Level.INFO, "Sending handshake Msg: " + sb.toString());
		return new String(sb);
	}

	public static String readHandshakeMsg(String msg) {
		if(msg == null || msg.length() != Const.HANDSHAKE_MSG_LEN)
			return "";

		int headerLen = Const.HANDSHAKE_HEADER.length();
		if(!Const.HANDSHAKE_HEADER.equals(msg.substring(0, headerLen)))
			return "";

		String neighborPeerId =  msg.substring(headerLen + Const.HANDSHAKE_ZERO_BITS_LEN);
		P2PLogger.getLogger().log(Level.INFO, "Receiving handshake Msg: " + msg + " from neighbor: ", neighborPeerId);
		return neighborPeerId;
	}

}
