package server;


public class ServerMain {

    public static void main(String[] argv) throws Exception {
        ServerFormController serverController = new ServerFormController();
        serverController.startListening();
    }
}
