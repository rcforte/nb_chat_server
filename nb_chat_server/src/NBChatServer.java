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
	
	public NBChatServer(int port) throws Exception {
		m_port = port;
		m_selector = Selector.open();
	}
	
	public void start() throws Exception {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.register(m_selector, SelectionKey.OP_ACCEPT);
		
		InetSocketAddress addr = new InetSocketAddress(m_port);
		ServerSocket server = serverChannel.socket();
		server.bind(addr);

		System.out.println("server started on port " + m_port);
		
		for (;;) {
			int nkeys = m_selector.select();
			if (nkeys == 0) {
				continue;
			}
			
			Iterator<SelectionKey> it = m_selector.selectedKeys().iterator();
			
			while (it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();
				
				if (!key.isValid()) {
					continue;
				}
				
				if (key.isAcceptable()) {
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
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);

		SelectionKey cliKey = socketChannel.register(m_selector, SelectionKey.OP_READ);
		m_clients.add(cliKey);
	}
	
	private void handleRead(SelectionKey client) throws Exception {
		SocketChannel socketChannel = (SocketChannel) client.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int size = socketChannel.read(buffer);

		if (size == -1) {
			client.cancel();
		} else {
			buffer.flip();
			
			Charset charset = Charset.defaultCharset();
			CharsetDecoder decoder = charset.newDecoder();
			CharBuffer charBuff = decoder.decode(buffer);
			
			System.out.println("received " + charBuff.toString());
			
			for (SelectionKey otherClient : m_clients) {
				if (otherClient != client) {
					otherClient.attach(charBuff.toString());
					otherClient.interestOps(SelectionKey.OP_WRITE);
				}
			}
		}
	}
	
	private void handleWrite(SelectionKey key) throws Exception {
		try {
			String message = (String) key.attachment();

			SocketChannel socketChannel = (SocketChannel) key.channel();
			socketChannel.write(ByteBuffer.wrap(message.getBytes()));

			key.attach(null);
			key.interestOps(SelectionKey.OP_READ);
		} catch (Exception e) {
			// OK, the guy may have disconnected
		}
	}
}
