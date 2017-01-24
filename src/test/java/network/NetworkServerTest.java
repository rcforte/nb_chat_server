package network;

import com.google.common.collect.Lists;
import network.echo.EchoService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkServerTest {
  Network server;

  @Before
  public void setup() throws IOException {
    server = new Network();
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

    List<Socket> sockets = Lists.newArrayList();
    for (int i = 0; i < 1000; i++) {
      Socket socket = new Socket("localhost", 9999);
      sockets.add(socket); // avoids being collected

      NetworkEvent networkEvent = blockingQueue.poll(1, TimeUnit.SECONDS);
      assertNotNull(networkEvent);
      assertEquals(NetworkEventType.ACCEPT, networkEvent.getType());
      assertNotNull(networkEvent.getSocketChannel());
    }
  }

  @Test
  public void serverRespondsToClients() throws Exception {
    String sentMessage = "Sample message";
    Encoder<String> encoder = new TokenEncoder("\n");
    server.addNetworkListener(new EchoService(encoder));

    List<Socket> sockets = Lists.newArrayList();
    for (int i = 0; i < 1000; i++) {
      Socket socket = new Socket("localhost", 9999);
      socket.getOutputStream().write(encoder.encode(sentMessage));
      sockets.add(socket); // avoids being collected

      byte[] bytes = new byte[100];
      int n = socket.getInputStream().read(bytes);
      bytes = new String(bytes, 0, n).getBytes();
      String receivedMessage = encoder.decode(bytes).get(0);

      assertEquals(sentMessage, receivedMessage);
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
