package nbserver.chat;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import nbserver.Handler;

public class ChatHandler implements Handler {
	private final List<SelectionKey> m_clients = new ArrayList<SelectionKey>();
	private final MessageMgr m_messageMgr = new MessageMgr();

	public void handleAccept(SelectionKey key) throws Exception {
		// Get the incoming socket connection and set it as non-blocking.
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);

		// Listen client writes.
		SelectionKey clientSelectionkey = socketChannel.register(key.selector(), SelectionKey.OP_READ);
		
		// Add key to list of clients. Messages will be broadcast to 
		// all clients.
		m_clients.add(clientSelectionkey);
	}
	
	public void handleRead(SelectionKey thisClient) throws Exception {
		System.out.println("handlingRead...");
		
		// Read bytes into the buffer.
		SocketChannel socketChannel = (SocketChannel) thisClient.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int size = socketChannel.read(buffer);

		// Check if the client disconnected.+
		if (size == -1) {
			handleDisconnect(thisClient);
		} else {
			// Convert the bytes into a string using default charset.
			buffer.flip();
			Charset charset = Charset.defaultCharset();
			CharsetDecoder decoder = charset.newDecoder();
			CharBuffer charBuff = decoder.decode(buffer);
			String msg = charBuff.toString();
	
			System.out.println("received " + msg);
			
			// Broadcast the message.
			for (SelectionKey client : m_clients) {
				if (client != thisClient) {
					m_messageMgr.addMessage(client, msg);

					// Start listening to writes.
					if (!client.isWritable()) {
						client.interestOps(SelectionKey.OP_WRITE);
					}
				}
			}
		}
	}

	public void handleDisconnect(SelectionKey key) {
		key.cancel();
		m_clients.remove(key);
	}
	
	public void handleWrite(SelectionKey key) throws Exception {
		System.out.println("handlingWrite...");
		try {
			// Get the pending messages for this client.
			List<Message> messages = m_messageMgr.getMessages(key);
			if (messages == null || messages.isEmpty()) {
				return;
			}
			
			// Write the messages.
			SocketChannel socketChannel = (SocketChannel) key.channel();
			for (Message m : messages) {
				String content = m.content();
				ByteBuffer buffer = ByteBuffer.wrap(content.getBytes());
				socketChannel.write(buffer);
			}

			// Listen to the next read.
			if (!key.isReadable()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (Exception e) {
			e.printStackTrace();
			handleDisconnect(key);
		}
	}
}
