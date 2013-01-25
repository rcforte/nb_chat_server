import nbchat.server.ChatHandler;
import nbchat.server.Dispatcher;
import nbchat.server.NBServer;

public class Main {
	public static void main(String[] args) throws Exception {
		ChatHandler handler = new ChatHandler();
		Dispatcher dispatcher = new Dispatcher(handler);
		NBServer server = new NBServer(dispatcher, 9999);
		server.start();
	}
}
