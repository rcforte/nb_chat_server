package nbserver;

import java.nio.channels.SelectionKey;

public interface Handler {
	void handleAccept(SelectionKey key) throws Exception;
	void handleRead(SelectionKey key) throws Exception;
	void handleWrite(SelectionKey key) throws Exception;
	void handleDisconnect(SelectionKey key) throws Exception;
}
