import chat.server.ChatServer;

public class Main {
	public static void main(String[] args) throws Exception {
		new ChatServer(9999).start();
	}
}
