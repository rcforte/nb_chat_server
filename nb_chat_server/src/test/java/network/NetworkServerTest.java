package network;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkServerTest {

    NonBlockingNetwork server;

    @Before
    public void setup() throws IOException {
        server = new NonBlockingNetwork();
        server.bind(9999);
    }

    @After
    public void shutdown() throws Exception {
        server.stop();
        Thread.sleep(300);
    }

    @Test
    public void serverAcceptsConnections() throws Exception {

        BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
        server.addNetworkListener(networkEvent -> blockingQueue.add(networkEvent));

        for (int i = 0; i < 1000; i++) {
            Socket socket = new Socket("localhost", 9999);

            NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull(networkEvent);
            assertEquals(NetworkEventType.ACCEPT, networkEvent.getType());
            assertNotNull(networkEvent.getSocketChannel());
        }
    }

    @Test
    public void serverRespondsToClients() throws Exception {

        server.addNetworkListener(networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.READ) {
                SocketChannel socketChannel = networkEvent.getSocketChannel();
                byte[] data = networkEvent.getData();
                server.send(socketChannel, data);
            }
        });

        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[100];
            Socket socket = new Socket("localhost", 9999);
            socket.getOutputStream().write("Sample message".getBytes());
            int n = socket.getInputStream().read(bytes);

            assertEquals("Sample message", new String(bytes, 0, n));
        }
    }

    @Test
    public void serverAcceptsClientsClose() throws Exception {

        BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
        server.addNetworkListener(networkEvent -> {
            if (networkEvent.getType() == NetworkEventType.DISCONNECT) {
                blockingQueue.add(networkEvent);
            }
        });

        for (int i = 0; i < 1000; i++) {
            Socket socket = new Socket("localhost", 9999);
            socket.close();

            NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
            assertNotNull(networkEvent);
            assertEquals(NetworkEventType.DISCONNECT, networkEvent.getType());
            assertNotNull(networkEvent.getSocketChannel());
        }
    }

}
