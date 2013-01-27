package nbchat.server;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChatTest {
	private ExecutorService m_executor = Executors.newFixedThreadPool(2);
	private ChatServer m_server;
	private BlockingChatClient m_client;
	private FakeChatListener m_clientListener;

	@Before
	public void setUp() throws Exception, InterruptedException {
		startServer();
		startClient();
	}

	@After
	public void tearDown() {
		m_client.stop();
		m_server.stop();
	}
	
	private void startClient() {
		m_clientListener = new FakeChatListener();
		m_client = new BlockingChatClient();
		m_client.addListener(m_clientListener);
		m_executor.execute(new Runnable() {
			@Override
			public void run() {
				m_client.connect();
			}
		});
		m_client.waitForResult();
	}

	private void startServer() 
		throws Exception, InterruptedException 
	{
		m_server = new ChatServer(9999);
		m_executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					m_server.start();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		TimeUnit.SECONDS.sleep(1);
	}

	@Test
	public void shouldConnect() {
		assertEquals(FakeChatListener.ON_CONNECTED, 
			m_clientListener.getResult());
	}

	@Test
	public void shouldFailConnectWhenServerIsStopped() {
		m_client.stop();
		m_server.stop();
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		startClient();
		assertEquals(FakeChatListener.ON_FAIL, 
			m_clientListener.getResult());
	}
	
	@Test
	public void shouldGetRooms() {
		m_client.getChatRooms();
		m_client.waitForResult();
		assertEquals(Arrays.asList("room1", "room2"), 
			m_clientListener.getResults());
	}
}
