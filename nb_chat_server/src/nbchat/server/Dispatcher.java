package nbchat.server;

import java.nio.channels.SelectionKey;

/**
 * The dispatcher is only responsible for dispatching I/O events to the handler.
 */
public class Dispatcher {
	private final Handler m_handler;

	public Dispatcher(Handler handler) {
		m_handler = handler;
	}

	public void dispatchEvent(SelectionKey key) {
		try {
			if (!key.isValid()) {
				return;
			}
			if (key.isAcceptable()) {
				m_handler.handleAccept(key);
			}
			if (key.isReadable()) {
				m_handler.handleRead(key);
			}
			if (key.isWritable()) {
				m_handler.handleWrite(key);
			}
		}
		catch (Exception e) {
			e.printStackTrace(); // don't blow up the server on error
		}
	}
}
