package org.networks.java.helper;

import org.networks.java.service.P2PLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
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
			P2PLogger.getLogger().log(Level.INFO, "Msg sent: " + msg);
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
			P2PLogger.getLogger().log(Level.INFO, "Msg received: " + msg);
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
		byte[] msgPayLoadLenBytes = new byte[Const.MSG_LEN_LEN];
		byte[] msgTypeBytes = new byte[Const.MSG_TYPE_LEN];

		int msgPayLoadLen = 0;
		int msgTypeLen = 0;
		byte msgType = -1;

		try {
			if(inStream.available() < 1)
				return;
			inStream.read(msgPayLoadLenBytes);
			ByteBuffer wrapped = ByteBuffer.wrap(msgPayLoadLenBytes);
			msgPayLoadLen = wrapped.getInt();
			msgTypeLen = inStream.read(msgTypeBytes);
			ByteBuffer byteBuffer = ByteBuffer.wrap(msgTypeBytes);
			msgType = byteBuffer.get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " receiving message with payload length: " + msgPayLoadLen + " and msgType: " + msgType + ".";
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
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received Choke message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendChokeMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.CHOKE).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending CHOKE message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readUnChokeMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received UnChoke message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendUnChokeMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.UNCHOKE).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending UnChoke message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readInterestedMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received Interested message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendInterestedMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.INTERESTED).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending Interested message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readNotInterestedMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received NotInterested message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendNotInterestedMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.NOT_INTERESTED).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending NotInterested message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);

	}

// ch
	public void readHaveMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received Have message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendHaveMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.HAVE).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending Have message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readRequestMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received Request message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendRequestMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.REQUEST).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending Request message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void readPieceMsg(int msgPayLoadLen) {
		byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

		try {
			int len = inStream.read(msgPayLoadBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String logMsg = "Peer" + " received Piece message" + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void sendPieceMsg(int msgPayLoadLen) {
		byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
		int index = 0;

		ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
		byteBuffer.putInt(msgPayLoadLen);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		int msgType = Const.MsgType.valueOf(Const.PIECE).ordinal();
		byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
		byteBuffer.put((byte)msgType);
		for(byte val: byteBuffer.array())
			msgBytes[index++] = val;

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending Piece message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
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

		String logMsg = "Peer" + " received Bit Field message: " + Arrays.toString(piecesPresent) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
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

		BitSet temp = new BitSet(msgPayLoadLen);
		for(int i = 0; i < 306 ; i++){
			temp.set(i, true);
		}

		byte[] t = temp.toByteArray();

		for(byte val: t)
			msgBytes[index++] = val;

		try {
			outStream.write(msgBytes);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		printByteArr(msgBytes);
		String logMsg = "Peer" + " sending Bit Field message: " + Arrays.toString(msgBytes) + ".";
		P2PLogger.getLogger().log(Level.INFO, logMsg);
	}

	public void printByteArr(byte[] msgBytes){
		for(byte val: msgBytes)
			System.out.print(val + " ");
	}
}
