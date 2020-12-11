package org.networks.java.helper;

import org.networks.java.model.PeerInfo;
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

    public MessageStream(Socket socket) {
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
            outStream.flush();
            inStream = new ObjectInputStream(socket.getInputStream());
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

        if (byteReadLen < 1)
            return "";

        String msg = new String(msgBytes, StandardCharsets.UTF_8);
        if (msg == null || msg.length() != Const.HANDSHAKE_MSG_LEN)
            return "";

        int headerLen = Const.HANDSHAKE_HEADER.length();
        if (!Const.HANDSHAKE_HEADER.equals(msg.substring(0, headerLen)))
            return "";

        String neighborPeerId = msg.substring(headerLen + Const.HANDSHAKE_ZERO_BITS_LEN);
        return neighborPeerId;
    }

    public boolean sendHandshakeMsg(String peerId) {
        StringBuilder sb = new StringBuilder();
        sb.append(Const.HANDSHAKE_HEADER);
        for (int i = 0; i < Const.HANDSHAKE_ZERO_BITS_LEN; i++)
            sb.append("0");
        sb.append(peerId);

        try {
            outStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void readMsg(PeerInfo neighborPeerInfo) {
        byte[] msgPayLoadLenBytes = new byte[Const.MSG_LEN_LEN];
        byte[] msgTypeBytes = new byte[Const.MSG_TYPE_LEN];

        int msgPayLoadLen = 0;
        int msgTypeLen = 0;
        byte msgType = -1;

        try {
            if (inStream.available() < 1)
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

        String logMsg = "Peer" + neighborPeerInfo.getPeerId() + " receiving message with payload length: " + msgPayLoadLen + " and msgType: " + msgType + ".";
        System.out.println(logMsg);
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
                readBitFieldMsg(neighborPeerInfo, msgPayLoadLen);
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
        System.out.println(logMsg);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
    }

    public void sendChokeMsg(int msgPayLoadLen) {
        byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
        int index = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
        byteBuffer.putInt(msgPayLoadLen);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.CHOKE).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
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

        String logMsg = "Peer" + " received UnChoke message.";
        P2PLogger.getLogger().log(Level.INFO, logMsg);
    }

    public void sendUnChokeMsg(int msgPayLoadLen) {
        byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
        int index = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
        byteBuffer.putInt(msgPayLoadLen);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.UNCHOKE).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
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
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.INTERESTED).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        try {
            outStream.write(msgBytes);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.NOT_INTERESTED).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        try {
            outStream.write(msgBytes);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ch
    public void readHaveMsg(int msgPayLoadLen) {
        byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

        try {
            int len = inStream.read(msgPayLoadBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String logMsg = "Peer" + " received Have message" + "." + Arrays.toString(msgPayLoadBytes);
        P2PLogger.getLogger().log(Level.INFO, logMsg);
    }

    public void sendHaveMsg(int msgPayLoadLen) {
        byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
        int index = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
        byteBuffer.putInt(msgPayLoadLen);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.HAVE).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        BitSet temp = new BitSet(msgPayLoadLen);
        for (int i = 0; i < msgPayLoadLen; i++) {
            temp.set(i, true);
        }
        byte[] t = temp.toByteArray();
        for (byte val : t) {
            msgBytes[index++] = val;
        }
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

    public int readRequestMsg(int msgPayLoadLen) {
        byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

        try {
            int len = inStream.read(msgPayLoadBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayToInt(msgPayLoadBytes);
    }

    public void sendRequestMsg(int msgPayLoadLen, int pieceIndex) {
        byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
        int index = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
        byteBuffer.putInt(msgPayLoadLen);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.REQUEST).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        intToByteArray(pieceIndex);
        for (byte val : intToByteArray(pieceIndex))
            msgBytes[index++] = val;

        try {
            outStream.write(msgBytes);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] readPieceMsg(int msgPayLoadLen) {
        byte[] data = new byte[msgPayLoadLen];
        try {
            inStream.readFully(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void sendPieceMsg(int pieceIndex, byte[] piece) {
        ByteBuffer buffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN + Const.MSG_LEN_LEN + piece.length);
        int msgType = Const.MsgType.valueOf(Const.PIECE).ordinal();

        System.out.println(piece.length);
        buffer.putInt(piece.length);
        buffer.put((byte) msgType);
        buffer.put(piece);

        try {
            outStream.write(buffer.array());
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean readBitFieldMsg(PeerInfo neighborPeerInfo, int msgPayLoadLen) {
        byte[] msgPayLoadBytes = new byte[msgPayLoadLen];

        try {
            int len = inStream.read(msgPayLoadBytes);
        } catch (IOException e) {
            e.printStackTrace();
            neighborPeerInfo.setPieceIndexes(new BitSet());
        }

        System.out.println(Arrays.toString(msgPayLoadBytes));
        neighborPeerInfo.setPieceIndexes(BitSet.valueOf(msgPayLoadBytes));
        System.out.println(neighborPeerInfo.getPieceIndexes().toString());
        return true;
    }

    public boolean sendBitFieldMsg(BitSet pieceIndexes) {
        int msgPayLoadLen = (int) Math.ceil((double) pieceIndexes.length() / 8);
        byte[] msgBytes = new byte[Const.MSG_LEN_LEN + Const.MSG_TYPE_LEN + msgPayLoadLen];
        int index = 0;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Const.MSG_LEN_LEN);
        byteBuffer.putInt(msgPayLoadLen);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        int msgType = Const.MsgType.valueOf(Const.BITFIELD).ordinal();
        byteBuffer = ByteBuffer.allocate(Const.MSG_TYPE_LEN);
        byteBuffer.put((byte) msgType);
        for (byte val : byteBuffer.array())
            msgBytes[index++] = val;

        for (byte val : pieceIndexes.toByteArray())
            msgBytes[index++] = val;

        try {
            outStream.write(msgBytes);
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        printByteArr(msgBytes);
        return true;
    }

    public void printByteArr(byte[] msgBytes) {
        for (byte val : msgBytes)
            System.out.print(val + " ");
        System.out.println();
    }

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
            (b[2] & 0xFF) << 8 |
            (b[1] & 0xFF) << 16 |
            (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a)
    {
        return new byte[] {
            (byte) ((a >> 24) & 0xFF),
            (byte) ((a >> 16) & 0xFF),
            (byte) ((a >> 8) & 0xFF),
            (byte) (a & 0xFF)
        };
    }
}
