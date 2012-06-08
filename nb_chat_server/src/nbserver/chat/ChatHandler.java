package nbserver.chat;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import nbserver.Handler;

/** handles chat requests */
public class ChatHandler implements Handler {
	private final List<SelectionKey> m_clients = new ArrayList<SelectionKey>();

	public void handleAccept(SelectionKey serverKey) throws Exception {
		// Setup client socket.
		ServerSocketChannel serverChannel = (ServerSocketChannel) serverKey.channel();
		SocketChannel channel = serverChannel.accept();
		channel.configureBlocking(false);

		// Interested in reads.
		SelectionKey readKey = channel.register(serverKey.selector(), SelectionKey.OP_READ);

		// Add client to chat.
		m_clients.add(readKey);
	}

	public void handleRead(SelectionKey clientKey) throws Exception {
		// Read message.
		SocketChannel channel = (SocketChannel) clientKey.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int size = channel.read(buffer);

		// Broadcast message.
		if(size != -1) {
			byte[] msgBytes = new byte[size];
			buffer.flip();
			buffer.get(msgBytes, 0, size);
			buffer.clear();
			for(SelectionKey client : m_clients) {
				if(client != clientKey) {
					((SocketChannel) client.channel()).write(ByteBuffer.wrap(msgBytes));
				}
			}
		}
		else {
			handleDisconnect(clientKey); // disconnected
		}
	}

	public void handleDisconnect(SelectionKey clientKey) {
		clientKey.cancel();
		m_clients.remove(clientKey);
	}

	public void handleWrite(SelectionKey clientKey) throws Exception {
		// noop
	}
}
