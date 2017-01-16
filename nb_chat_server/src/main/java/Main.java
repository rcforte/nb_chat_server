import chat.server.ChatServer;

public class Main {

    public static void main(String[] args) throws Exception {

        ChatServer chatServer = new ChatServer(9999);
        chatServer.start();
    }
}
