package app.server.thread;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ServerThread extends Thread {
    private final List<ClientThread> clients;
    private final ServerSocket serverRtspSocket;

    public ServerThread() throws IOException {
        clients = new ArrayList<ClientThread>();
        serverRtspSocket = new ServerSocket(1051);
    }

    @Override
    public void run() {
        while(true) {
            try {
                Socket clientRtspSocket = this.serverRtspSocket.accept();
                ClientThread clientThread = new ClientThread(clientRtspSocket);
                clientThread.start();

                clients.add(clientThread);
            } catch (IOException e) {
                log.error("Error while accepting new client, ex: ", e);
            }
        }
    }
}
