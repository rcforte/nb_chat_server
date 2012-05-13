package nbserver;

import java.nio.channels.SelectionKey;

public class Dispatcher {
	private final Handler m_handler;
	
	public Dispatcher(Handler handler) {
		m_handler = handler;
	}
	
	public void dispatchEvent(SelectionKey key) throws Exception {
		// Handle the event.
		if (!key.isValid()) {
			key.cancel();
		} else if (key.isAcceptable()) {
			m_handler.handleAccept(key);
		} else if (key.isReadable()) {
			m_handler.handleRead(key);
		} else if (key.isWritable()) {
			m_handler.handleWrite(key);
		} else {
			System.out.println("unknown state: " + key);
		}
	}
}
