package chat.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class WriteTicket {
	private final SocketChannel m_channel;
	private final ByteBuffer m_buffer;
	
	public WriteTicket(SocketChannel channel, ByteBuffer buffer) {
		m_channel = channel;
		m_buffer = buffer;
	}

	public void setWriteInterest(Selector m_selector) {
		m_channel.keyFor(m_selector).interestOps(SelectionKey.OP_WRITE);
	}

	public ByteBuffer getBuffer() {
		return m_buffer;
	}
}
