package org.networks.java.helper;

import org.networks.java.service.P2PLogger;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;

public class MessageStream {

	private DataInputStream inStream;
	private DataOutputStream outStream;

	public DataInputStream getInputStream() {
		return inStream;
	}

	public DataOutputStream getOutputStream() {
		return outStream;
	}

	public MessageStream(Socket socket) {
		try {
			outStream = new DataOutputStream(socket.getOutputStream());
			inStream = new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			P2PLogger.getLogger().log(Level.SEVERE, e.getMessage());
		}
	}

	public void printByteArr(byte[] msgBytes) {
		for (byte val : msgBytes)
			System.out.print(val + " ");
		System.out.println();
	}

	public static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF |
			(b[2] & 0xFF) << 8 |
			(b[1] & 0xFF) << 16 |
			(b[0] & 0xFF) << 24;
	}

	public static byte[] intToByteArray(int a) {
		return new byte[]{
			(byte) ((a >> 24) & 0xFF),
			(byte) ((a >> 16) & 0xFF),
			(byte) ((a >> 8) & 0xFF),
			(byte) (a & 0xFF)
		};
	}

	public int read4ByteIntData() throws IOException {
		byte[] data = new byte[Constants.MSG_LEN_LEN];
		inStream.read(data);
		return byteArrayToInt(data);
	}

	public int read1ByteMsgType() throws IOException {
		byte[] data = new byte[Constants.MSG_TYPE_LEN];
		inStream.read(data);
		return byteArrayToInt(data);
	}
}
