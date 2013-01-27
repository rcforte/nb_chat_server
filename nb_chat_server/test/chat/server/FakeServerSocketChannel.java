package chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

class FakeServerSocketChannel extends ServerSocketChannel {
	public SocketChannel m_socketChannel;

	protected FakeServerSocketChannel() {
		super(null);
	}
	
	@Override
	public SocketAddress getLocalAddress() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServerSocket socket() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SocketChannel accept() throws IOException {
		return m_socketChannel;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}