package network;

import com.google.common.collect.Lists;
import network.echo.EchoService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkClientTest {
    NonBlockingNetwork server;

    @Before
    public void setup() throws IOException {
        EchoService echoService =
                new EchoService(new TokenMessageEncoder("\n"));

        server = new NonBlockingNetwork();
        server.addNetworkListener(echoService);
        server.bind(9999);
    }

    @After
    public void teardown() throws Exception {
        server.stop();
        Thread.sleep(1000);
    }

    @Test
    public void receivesResponseUsingListener() throws Exception {
        BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
        TokenMessageEncoder encoder = new TokenMessageEncoder("\n");
        NetworkClient networkClient = new NetworkClient(new NonBlockingNetwork());
        networkClient.addNetworkListener(networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.READ) {
                blockingQueue.add(networkEvent);
            }
        });
        networkClient.connect("localhost", 9999);
        Thread.sleep(1000);

        networkClient.send(encoder.encode("This is a test"));

        NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
        assertNotNull(networkEvent);
        assertEquals(NetworkEventType.READ, networkEvent.getType());
        assertEquals("This is a test", encoder.decode(networkEvent.getData()).get(0));
    }

    @Test
    public void receivesMessage() throws Exception {
        BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
        NetworkListener networkListener = networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.READ) {
                blockingQueue.add(networkEvent);
            }
        };
        TokenMessageEncoder encoder = new TokenMessageEncoder("\n");
        List<String> sent = Lists.newArrayList("Message1", "Message2", "Message3");

        NetworkClient networkClient = new NetworkClient(new NonBlockingNetwork());
        networkClient.addNetworkListener(networkListener);
        networkClient.connect("localhost", 9999);
        Thread.sleep(1000);

        sent.stream().forEach(message ->
                server.broadcast(encoder.encode(message))
        );

        List<String> received = Lists.newArrayList();
        while (received.size() != 3) {
            NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull(networkEvent);
            assertEquals(NetworkEventType.READ, networkEvent.getType());
            received.addAll(encoder.decode(networkEvent.getData()));
        }

        assertEquals("Wrong messages received from server", sent, received);
    }
}

