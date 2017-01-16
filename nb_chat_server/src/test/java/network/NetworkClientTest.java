package network;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkClientTest {

    NonBlockingNetwork server;

    @Before
    public void setup() throws IOException {
        server = new NonBlockingNetwork();
        EchoNetworkListener networkListener = new EchoNetworkListener(server);
        server.addNetworkListener(networkListener);
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
        NetworkClient networkClient = new NetworkClient(new NonBlockingNetwork());
        networkClient.addNetworkListener(networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.READ) {
                blockingQueue.add(networkEvent);
            }
        });
        networkClient.connect("localhost", 9999);
        Thread.sleep(1000);
        networkClient.send("This is a test".getBytes());

        NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);

        assertNotNull(networkEvent);
        assertEquals(NetworkEventType.READ, networkEvent.getType());
        assertEquals("This is a test", new String(networkEvent.getData()));
    }

    @Test
    public void receivesMessage() throws Exception {

        class MessageReceiver implements NetworkListener {

            public void onEvent(NetworkEvent networkEvent) {

                if (networkEvent.getType() == NetworkEventType.READ) {

                    // receive bytes
                    // add to accumulator
                    // make string from bytes
                    // split string by separator
                    // try to parse message with bytes in accumulator
                }
            }
        }

        BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
        NetworkClient networkClient = new NetworkClient(new NonBlockingNetwork());
        networkClient.addNetworkListener(networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.READ) {
                blockingQueue.add(networkEvent);
            }
        });
        networkClient.connect("localhost", 9999);
        Thread.sleep(1000);

        int messageCount = 3;
        for (int i = 0; i < messageCount; i++) {

            server.broadcast(String.format("Message %d", i).getBytes());
        }

        for (int i = 0; i < messageCount; i++) {

            NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull(networkEvent);
            assertEquals(NetworkEventType.READ, networkEvent.getType());
            assertEquals(String.format("Message %d", i), new String(networkEvent.getData()));
        }
    }
}

class NetworkClient {

    private final List<NetworkListener> networkListeners = Lists.newCopyOnWriteArrayList();
    private final NonBlockingNetwork network;
    private final NetworkListener networkListener = networkEvent -> {
        if (networkEvent.getType() == NetworkEventType.CONNECT) {
            socketChannel = networkEvent.getSocketChannel();
        }
    };

    private SocketChannel socketChannel;

    public NetworkClient(NonBlockingNetwork network) {
        this.network = network;
        this.network.addNetworkListener(networkListener);
    }

    public void addNetworkListener(NetworkListener networkListener) {
        this.network.addNetworkListener(networkListener);
    }

    public void removeNetworkListener(NetworkListener networkListener) {
        this.network.removeNetworkListener(networkListener);
    }

    public void connect(String host, int port) throws IOException {
        this.network.connect(host, port);
    }

    public void send(byte[] data) {
        checkArgument(data != null, "data cannot be null");
        checkArgument(data.length > 0, "data cannot be empty");
        checkNotNull(socketChannel, "client not connected");

        this.network.send(socketChannel, data);
    }
}
