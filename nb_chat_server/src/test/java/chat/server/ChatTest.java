package chat.server;

import chat.client.ChatClient;
import chat.common.RequestMessage;
import chat.common.ResponseMessage;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static chat.common.RequestMessageType.GET_ROOMS;
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
    }

    @After
    public void tearDown() throws IOException {
        chatClient.stop();
        chatServer.stop();
    }


    @Test
    public void sendsWithProcessor() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(GET_ROOMS);
        requestMessage.setCorrelationId(correlationId);

        BlockingQueue<ResponseMessage> blockingQueue = new LinkedBlockingDeque<>();
        chatClient.send(requestMessage, responseMessage -> blockingQueue.offer(responseMessage));

        ResponseMessage responseMessage = blockingQueue.poll(3L, TimeUnit.SECONDS);
        assertNotNull("server did not return response", responseMessage);

        List<String> expected = Lists.newArrayList("room1", "room2");
        assertEquals(expected, responseMessage.getRooms());
    }

    @Test
    public void sendsGetRoomsRequest() throws Exception {
        BlockingQueue<ResponseMessage> blockingQueue = new LinkedBlockingDeque<>();
        chatClient.getChatRooms(responseMessage -> blockingQueue.offer(responseMessage));

        ResponseMessage responseMessage = blockingQueue.poll(3L, TimeUnit.SECONDS);
        assertNotNull("server did not return response", responseMessage);

        List<String> expected = Lists.newArrayList("room1", "room2");
        assertEquals(expected, responseMessage.getRooms());
    }
}
