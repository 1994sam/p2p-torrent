package org.networks.java.helper;

public class Const {

	public final static int BYTE_LEN = 8;
	public final static int PEER_ID_LEN = 4;
	public final static int MSG_LEN_LEN = 4;
	public final static int MSG_TYPE_LEN = 1;

	public final static int HANDSHAKE_ZERO_BITS_LEN = 10;
	public final static int HANDSHAKE_MSG_LEN = 32;
	public final static int PIECE_INDEX_PAYLOAD_LEN = 4;
	public final static String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

	public static final String COMMON_CFG_FILE_NAME = "Common.cfg";
	public static final String PEER_INFO_CFG_FILE_NAME = "PeerInfoDummy.cfg";
	public static final String USER_DIR_PATH = "user.dir";
	public static final String FILE_DIR_PREXFIX_PATH = "peer_";

	public static final String LOG_FILE_PATH = "log_peer_";
	public static final String LOG_FILE_EXTENSTION = ".log";


	public static final String CHOKE = "CHOKE";
	public static final String UNCHOKE = "UNCHOKE";
	public static final String INTERESTED = "INTERESTED";
	public static final String NOT_INTERESTED = "NOT_INTERESTED";
	public static final String HAVE = "HAVE";
	public static final String BITFIELD = "BITFIELD";
	public static final String REQUEST = "REQUEST";
	public static final String PIECE = "PIECE";

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
