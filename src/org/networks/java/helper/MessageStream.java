package org.networks.java.helper;

import org.networks.java.service.P2PLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class MessageStream {

	private ObjectInputStream inStream;
	private ObjectOutputStream outStream;

	private String msg;

	public ObjectInputStream getInStream() {
		return inStream;
	}

	public ObjectOutputStream getOutStream() {
		return outStream;
	}

	public String getMsg() {
		return msg;
	}

	public MessageStream(Socket socketCon) {
		try {
			outStream = new ObjectOutputStream(socketCon.getOutputStream());
			outStream.flush();
			inStream = new ObjectInputStream(socketCon.getInputStream());
		} catch (IOException e) {
			P2PLogger.getLogger().log(Level.SEVERE, e.getMessage());
		}
	}

	/*public void sendMsg(String msg) {
		try {
			outStream.write(msg.getBytes(StandardCharsets.UTF_8));
			outStream.flush();
//			P2PLogger.getLogger().log(Level.INFO, "Msg sent: " + msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String readMsg(int msgLen) {
		String msg = "";
		try {
			byte[] msgBytes = new byte[msgLen];
			int len = inStream.read(msgBytes);
			msg = new String(msgBytes, StandardCharsets.UTF_8);
//			P2PLogger.getLogger().log(Level.INFO, "Msg received: " + msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return msg;
	}*/

	public String readHandshakeMsg(int msgLen) {
		byte[] msgBytes = new byte[msgLen];
		int byteReadLen = 0;

		try {
			byteReadLen = inStream.read(msgBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(byteReadLen < 1)
			return "";

		String msg = new String(msgBytes, StandardCharsets.UTF_8);
		if(msg == null || msg.length() != Const.HANDSHAKE_MSG_LEN)
			return "";

		int headerLen = Const.HANDSHAKE_HEADER.length();
		if(!Const.HANDSHAKE_HEADER.equals(msg.substring(0, headerLen)))
			return "";

		String neighborPeerId =  msg.substring(headerLen + Const.HANDSHAKE_ZERO_BITS_LEN);
		String logMsg = "Peer " + neighborPeerId + " sent handshake message: " + msg + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		return neighborPeerId;
	}

	public void sendHandshakeMsg(String peerId) {
		StringBuilder sb = new StringBuilder();
		sb.append(Const.HANDSHAKE_HEADER);
		for(int i = 0; i < Const.HANDSHAKE_ZERO_BITS_LEN; i++)
			sb.append("0");
		sb.append(peerId);
		String logMsg = "Peer " + peerId + " sending handshake message: " + sb.toString() + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);

		try {
			outStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readMsg() {
		byte[] msgPayLoadLenBytes = new byte[Const.MSG_TYPE_LEN];
		byte[] msgTypeBytes = new byte[Const.MSG_TYPE_LEN];

		int msgPayLoadLen = 0;
		int msgTypeLen = 0;
		int msgType = -1;

		try {
			if(inStream.available() < 1)
				return;
			msgPayLoadLen = inStream.read(msgPayLoadLenBytes);
			msgTypeLen = inStream.read(msgTypeBytes);
			ByteBuffer byteBuffer = ByteBuffer.wrap(msgTypeBytes);
			msgType = byteBuffer.getInt();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " receiving message with payload length: " + msgPayLoadLen +
			" and msgType: " + Const.MsgType.getMsgType(msgType) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
		switch (msgType) {
			case 0:
				readChokeMsg(msgPayLoadLen);
				break;

			case 1:
				readUnChokeMsg(msgPayLoadLen);
				break;

			case 2:
				readInterestedMsg(msgPayLoadLen);
				break;

			case 3:
				readNotInterestedMsg(msgPayLoadLen);
				break;

			case 4:
				readHaveMsg(msgPayLoadLen);
				break;

			case 5:
				readBitFieldMsg(msgPayLoadLen);
				break;

			case 6:
				readRequestMsg(msgPayLoadLen);
				break;

			case 7:
				readPieceMsg(msgPayLoadLen);
				break;
		}
	}

	public void readChokeMsg(int msgPayLoadLen) {

	}

	public void readUnChokeMsg(int msgPayLoadLen) {

	}

	public void readInterestedMsg(int msgPayLoadLen) {

	}

	public void readNotInterestedMsg(int msgPayLoadLen) {

	}

	public void readHaveMsg(int msgPayLoadLen) {

	}

	public void readBitFieldMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];
		boolean[] piecesPresent = new boolean[msgPayLoadLen];
		int index = 0;

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for(byte val: msgPayLoadBytes)
			piecesPresent[index++] = val == 1;
	}

	public void sendBitFieldMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.BITFIELD).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		byteBuffer = ByteBuffer.allocate(msgPayLoadLen);
		byteBuffer.putInt(306);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " sending Bit Field message: " + msgBytes.toString() + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readRequestMsg(int msgPayLoadLen) {

	}

	public void readPieceMsg(int msgPayLoadLen) {

	}

}
