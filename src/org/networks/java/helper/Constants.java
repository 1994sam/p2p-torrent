package org.networks.java.helper;

public class Constants {

    public final static int PROCESS_EXIT_CODE = 0;
    public final static int PROCESS_STALL_INTERVAL = 5000;

    public final static long SEC_TO_MILLI_SEC = 1000L;

    public final static int BYTE_LEN = 8;
    public final static int PEER_ID_LEN = 4;
    public final static int MSG_LEN_0 = 0;
    public final static int MSG_LEN_LEN = 4;
    public final static int MSG_TYPE_LEN = 1;
    public final static int PIECE_INDEX_PAYLOAD_LEN = 4;

    public final static int HANDSHAKE_ZERO_BITS_LEN = 10;
    public final static int HANDSHAKE_MSG_LEN = 32;
    public final static String HANDSHAKE_HEADER = "SHARE_FILE";

    public static final String COMMON_CFG_FILE_NAME = "Common.cfg";
    public static final String PEER_INFO_CFG_FILE_NAME = "PeerInfoDummy.cfg";
    public static final String USER_DIR_PATH = "user.dir";
    public static final String FILE_DIR_PREFIX_PATH = "peer_";

    public static final String LOG_FILE_PATH = "log_peer_";
    public static final String LOG_FILE_EXTENSION = ".log";

    public enum MessageType {
        CHOKE("CHOKE"),
        UNCHOKE("UNCHOKE"),
        INTERESTED("INTERESTED"),
        NOT_INTERESTED("NOT_INTERESTED"),
        HAVE("HAVE"),
        BITFIELD("BITFIELD"),
        REQUEST("REQUEST"),
        PIECE("PIECE");

        private String name;

        public static MessageType getMessageValue(int typeIdx) {
            return MessageType.values()[typeIdx];
        }

        MessageType(String name) {
            this.name = name;
        }
    }
}
