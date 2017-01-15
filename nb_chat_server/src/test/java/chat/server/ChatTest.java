package chat.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ChatTest {
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private ChatServer chatServer;
    private BlockingChatClient blockingChatClient;
    private FakeChatListener fakeChatListener;

    @Before
    public void setUp() throws Exception, InterruptedException {
        startServer();
        startClient();
    }

    @After
    public void tearDown() {
        blockingChatClient.stop();
        chatServer.stop();
    }


    @Test
    public void shouldConnect() {
        assertEquals(FakeChatListener.ON_CONNECTED, fakeChatListener.getResult());
    }

    @Test
    public void shouldFailConnectWhenServerIsStopped() {
        blockingChatClient.stop();
        chatServer.stop();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        startClient();
        assertEquals(FakeChatListener.ON_FAIL, fakeChatListener.getResult());
    }

    @Test
    public void shouldGetRooms() {
        blockingChatClient.getChatRooms();
        blockingChatClient.waitForResult();
        assertEquals(Arrays.asList("room1", "room2"), fakeChatListener.getResults());
    }

    private void startClient() {
        fakeChatListener = new FakeChatListener();
        blockingChatClient = new BlockingChatClient();
        blockingChatClient.addListener(fakeChatListener);
        executorService.execute(() -> blockingChatClient.connect());
        blockingChatClient.waitForResult();
    }

    private void startServer() throws Exception, InterruptedException {
        chatServer = new ChatServer(9999);
        chatServer.start();
        TimeUnit.SECONDS.sleep(1);
    }

}
