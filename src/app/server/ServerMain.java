package app.server;


public class ServerMain {

    public static void main(String[] argv) throws Exception {
        Server server = new Server();
        server.startListening();
    }
}
