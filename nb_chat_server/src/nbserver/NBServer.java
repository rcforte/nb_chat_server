package nbserver;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

public class NBServer { 
	private final int m_port;
	private final Selector m_selector;
	private final Dispatcher m_dispatcher;
	
	public NBServer(Dispatcher dispatcher, int port) throws Exception {
		m_dispatcher = dispatcher;
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
		
		// Wait for IO events...
		for (;;) {
			int nkeys = m_selector.select();
			
			// Ignore false events.
			if (nkeys == 0) {
				continue;
			}
			
			// Handle all IO events ready for processing.
			for (Iterator<SelectionKey> it = m_selector.selectedKeys().iterator(); it.hasNext(); ) {
				// Get and consume the event.
				SelectionKey key = it.next();
				it.remove();
				
				dispatchEvent(key);
			}
		}
	}

	private void dispatchEvent(SelectionKey key) throws Exception {
		m_dispatcher.dispatchEvent(key);
	}
}
