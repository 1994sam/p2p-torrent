package org.networks.java;

import org.networks.java.helper.PeerConfig;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.util.ArrayList;

public class RemotePeerStarter {
	public static void main(String[] args) {

		String workingDir = System.getProperty("user.dir");

		try {
			for (PeerInfo info : new PeerConfig().getPeerInfo()) {
				Runtime.getRuntime().exec("ssh " + info.getHostName() + " cd " + workingDir + " ; " + "java org.networks.java.PeerProcess " + info.getPeerId() );
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
