import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NBChatServer { 
	private final int m_port;
	private final Selector m_selector;
	private final List<SelectionKey> m_clients = new ArrayList<SelectionKey>();
	private final MessageMgr m_messageMgr = new MessageMgr();
	
	public NBChatServer(int port) throws Exception {
		m_port = port;
		m_selector = Selector.open();
	}
	
	public void start() throws Exception {
		// Setup the server channel as non-blocking.
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		
		// Listen to client connections.
		serverChannel.register(m_selector, SelectionKey.OP_ACCEPT);
		
		// Bind the server to localhost.
		InetSocketAddress addr = new InetSocketAddress(m_port);
		ServerSocket server = serverChannel.socket();
		server.bind(addr);

		System.out.println("server started on port " + m_port);
		
		// Listen for IO events
		for (;;) {
			int nkeys = m_selector.select();
			
			// Ignore false events.
			if (nkeys == 0) {
				continue;
			}
			
			Iterator<SelectionKey> it = m_selector.selectedKeys().iterator();
			while (it.hasNext()) {
				// Get and consume the event identifier.
				SelectionKey key = it.next();
				it.remove();
				
				// Handle the event.
				if (!key.isValid()) {
					continue;
				} else if (key.isAcceptable()) {
					handleAccept(key);
				} else if (key.isReadable()) {
					handleRead(key);
				} else if (key.isWritable()) {
					handleWrite(key);
				}
			}
		}
	}
	
	private void handleAccept(SelectionKey key) throws Exception {
		// Get the incoming socket connection and set it as non-blocking.
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);

		// Listen client writes.
		SelectionKey clientSelectionkey = socketChannel.register(m_selector, SelectionKey.OP_READ);
		
		// Add key to list of clients. Messages will be broadcast to 
		// all clients.
		m_clients.add(clientSelectionkey);
	}
	
	private void handleRead(SelectionKey thisClient) throws Exception {
		System.out.println("handlingRead...");
		
		// Read bytes into the buffer.
		SocketChannel socketChannel = (SocketChannel) thisClient.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int size = socketChannel.read(buffer);

		// Check if the client disconnected.
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
					//client.attach(msg); ---> this would cause memory leaks.
					m_messageMgr.addMessage(client, msg);

					// Start listening to writes.
					if (!client.isWritable()) {
						client.interestOps(SelectionKey.OP_WRITE);
					}
				}
			}
		}
	}

	private void handleDisconnect(SelectionKey key) {
		key.cancel();
		m_clients.remove(key);
	}
	
	private void handleWrite(SelectionKey key) throws Exception {
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
				socketChannel.write(ByteBuffer.wrap(m.content().getBytes()));
			}

			// Listen to the next read.
			key.interestOps(SelectionKey.OP_READ);
		} catch (Exception e) {
			// OK, the guy may have disconnected
			handleDisconnect(key);
		}
	}
}
