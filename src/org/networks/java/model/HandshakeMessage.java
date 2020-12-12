package org.networks.java.model;


public class HandshakeMessage {

	private byte[] zeroBits;
	private static String headerString = "P2PFILESHARINGPROJ";
	private String header;
	private String peerID;

	public HandshakeMessage(String peerID) {
		this.header = headerString;
		this.zeroBits = new byte[10];
		this.peerID = peerID;
	}

	@Override
	public String toString(){
		return (headerString + new String(zeroBits) + peerID);
	}
}