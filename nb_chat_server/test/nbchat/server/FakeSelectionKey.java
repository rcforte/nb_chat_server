package nbchat.server;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

class FakeSelectionKey extends SelectionKey {
	public FakeServerSocketChannel m_serverSocketChannel;
	public Selector m_selector;
	
	public FakeSelectionKey() {
		try {
			m_selector = Selector.open();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public SelectableChannel channel() {
		// TODO Auto-generated method stub
		return m_serverSocketChannel;
	}

	@Override
	public Selector selector() {
		// TODO Auto-generated method stub
		return m_selector;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int interestOps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SelectionKey interestOps(int ops) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int readyOps() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}