package org.networks.java.service;

import org.networks.java.helper.MessageStream;
import org.networks.java.model.PeerInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class Server implements Runnable {

    Peer peer;

    public Server() {
    }

    public Server(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(peer.getPeerInfo().getPortNumber())) {
            while (true) {
                Socket socket = serverSocket.accept();
                MessageStream msgStream = new MessageStream(socket);
                String neighborPeerId = getNeighborPeerId(msgStream);
                Client client = createNewClient(socket, msgStream, neighborPeerId);
                P2PLogger.getLogger().log(Level.INFO, "Peer [" + peer.getPeerInfo().getPeerId() + "] is connected from Peer [" + neighborPeerId + "].");

                new Thread(client).start();
            }
        } catch (IOException e) {
        }

    }

    private Client createNewClient(Socket socket, MessageStream msgStream, String neighborPeerId) {
        PeerInfo neighbor = new PeerInfo(neighborPeerId, socket.getInetAddress().getHostAddress(), socket.getPort(), false);
        Client client = new Client(peer, neighbor, socket, msgStream);
        peer.addClient(client);
        return client;
    }

    private String getNeighborPeerId(MessageStream msgStream) throws IOException {
        String message = msgStream.getInputStream().readUTF();
        return message.substring(20, 24); //TODO change
    }
}
