import chat.server.ChatServer;
import chat.server.NonBlockingNetwork;

public class Main {
	public static void main(String[] args) throws Exception {
		ChatServer chatServer = new ChatServer(9999);
		chatServer.start();
	}
}
