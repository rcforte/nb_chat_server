package chat.server;

import chat.client.ChatClient;
import chat.common.ResponseMessage;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChatTest {
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    private ChatServer chatServer;
    private ChatClient chatClient;

    @Before
    public void setUp() throws Exception {
        chatServer = new ChatServer(9999);
        chatServer.start();

        chatClient = new ChatClient("localhost", 9999);
        chatClient.connect();
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws IOException{
        chatServer.stop();
    }


    @Test
    @Ignore
    public void shouldConnect() {
        //assertEquals(FakeChatListener.ON_CONNECTED, fakeChatListener.getResult());
    }

    @Test
    @Ignore
    public void shouldFailConnectWhenServerIsStopped() throws IOException{
        chatServer.stop();
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        startClient();
//        assertEquals(FakeChatListener.ON_FAIL, fakeChatListener.getResult());
    }

    @Test
    public void sendsGetRoomsRequest() throws Exception {
        BlockingQueue<ResponseMessage> blockingQueue = new LinkedBlockingDeque<>();
        chatClient.addChatListener(responseMessage -> blockingQueue.offer(responseMessage));
        chatClient.getChatRooms();

        ResponseMessage responseMessage = blockingQueue.poll(3L, TimeUnit.SECONDS);
        assertNotNull("server did not return response", responseMessage);

        List<String> expected = Lists.newArrayList("room1", "room2");
        assertEquals(expected, responseMessage.getRooms());
    }
}
