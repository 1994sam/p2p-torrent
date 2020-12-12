package org.networks.java.model;

import java.util.BitSet;

public class PeerInfo {

    private final String peerID;
    private final String hostName;
    private final int portNumber;
    private boolean filePresent;
    private BitSet pieceIndexes;
    private int missingPieces;

    public String getPeerID() {
        return peerID;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public BitSet getPieceIndexes() {
        return pieceIndexes;
    }

    public void setPieceIndexes(BitSet pieceIndexes) {
        this.pieceIndexes = pieceIndexes;
    }

    public boolean isFilePresent() {
        return filePresent;
    }

    public void setFilePresent(boolean filePresent) {
        this.filePresent = filePresent;
    }

    public int getMissingPieces() {
        return missingPieces;
    }

    public void setMissingPieces(int missingPieces) {
        this.missingPieces = missingPieces;
    }

    public PeerInfo() {
        this(null);
    }

    public PeerInfo(String peerID) {
        this(peerID, null, -1, false);
    }

    public PeerInfo(String peerID, String hostName, int port, boolean filePresent) {
        this.peerID = peerID;
        this.hostName = hostName;
        this.portNumber = port;
        this.filePresent = filePresent;
        this.pieceIndexes = new BitSet();
        this.missingPieces = 0;
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "peerID='" + peerID + '\'' +
                ", hostName='" + hostName + '\'' +
                ", portNumber=" + portNumber +
                ", filePresent=" + filePresent +
                ", pieceIndexes=" + pieceIndexes.toString() +
                ", missingPieces=" + missingPieces +
                '}';
    }
}
