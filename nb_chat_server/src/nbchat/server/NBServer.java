package nbchat.server;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/** non blocking server */
public class NBServer {
	private final int m_port;
	private final Selector m_selector;
	private final Dispatcher m_dispatcher;

	public NBServer(Dispatcher dispatcher, int port) throws Exception {
		m_dispatcher = dispatcher;
		m_port = port;
		m_selector = Selector.open();
	}

	/** performs NIO plumbing */
	public void start() throws Exception {
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.register(m_selector, SelectionKey.OP_ACCEPT);

		ServerSocket server = channel.socket();
		server.bind(new InetSocketAddress(m_port));
		System.out.println("server started on port " + m_port);

		for (;;) {
			if (m_selector.select() == 0) {
				continue;
			}
			for (Iterator<SelectionKey> it = m_selector.selectedKeys().iterator(); it.hasNext();) {
				SelectionKey key = it.next();
				it.remove();
				m_dispatcher.dispatchEvent(key);
			}
		}
	}
}
