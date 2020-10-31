package org.networks.java.helper;

import org.networks.java.model.PeerInfo;

public class Const {

	public final static int BYTE_LEN = 8;
	public final static int PEER_ID_LEN = 4;
	public final static int MSG_LEN_LEN = 4;
	public final static int MSG_TYPE_LEN = 1;

	public final static int HANDSHAKE_ZERO_BITS_LEN = 10;
	public final static int HANDSHAKE_MSG_LEN = 32;
	public final static String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

	public static final String COMMON_CFG_FILE_NAME = "Common.cfg";
	public static final String PEER_INFO_CFG_FILE_NAME = "PeerInfo.cfg";
	public static final String USER_DIR_PATH = "user.dir";

	public static final String LOG_FILE_PATH = "log_peer_";
	public static final String LOG_FILE_EXTENSTION = ".log";


	public enum MsgType {
		CHOKE,
		UNCHOKE,
		INTERESTED,
		NOT_INTERESTED,
		HAVE,
		BITFIELD,
		REQUEST,
		PIECE;

		public static MsgType getMsgType(int typeIdx) {
			return MsgType.values()[typeIdx];
		}
	}


}