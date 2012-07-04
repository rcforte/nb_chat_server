import nbserver.Dispatcher;
import nbserver.NBServer;
import nbserver.chat.ChatHandler;

/**
 * Entry point for the non-blocking server.
 */
public class Main {
	/**
	 * Setup the handler, the dispatcher, and launch the server.
	 */
	public static void 
	main(String[] args) throws Exception 
	{
		ChatHandler handler = new ChatHandler();
		Dispatcher dispatcher = new Dispatcher(handler);
		NBServer server = new NBServer(dispatcher, 9000);
		server.start();
	}
}
