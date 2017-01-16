package network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Exposes I/O event handling operations. Classes implementing this interface will be responsible for application specific logic.
 */
public interface Handler {
	void handleAccept(SelectionKey key) throws IOException;

	void handleConnect(SelectionKey key) throws IOException;

	void handleRead(SelectionKey key) throws IOException;

	void handleWrite(SelectionKey key) throws IOException;
}
