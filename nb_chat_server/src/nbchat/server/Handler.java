package nbchat.server;

import java.nio.channels.SelectionKey;

/**
 * Exposes I/O event handling operations. Classes implementing this interface will be responsible for application specific logic.
 */
public interface Handler {
	void handleAccept(SelectionKey key) throws Exception;

	void handleRead(SelectionKey key) throws Exception;

	void handleWrite(SelectionKey key) throws Exception;
}
