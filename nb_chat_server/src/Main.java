import nbserver.Dispatcher;
import nbserver.NBServer;
import nbserver.chat.ChatHandler;

public class Main {
	public static void main(String[] args) throws Exception {
		ChatHandler handler = new ChatHandler();
		Dispatcher dispatcher = new Dispatcher(handler);
		NBServer server = new NBServer(dispatcher, 9000);
		server.start();
	}
}
