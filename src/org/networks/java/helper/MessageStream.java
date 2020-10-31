package org.networks.java.helper;

import org.networks.java.service.P2PLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

	public void sendMsg(String msg) {
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
	}


}
