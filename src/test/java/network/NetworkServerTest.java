package network;

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

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.sleep;
import static network.NetworkEventType.ACCEPT;
import static network.NetworkEventType.DISCONNECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkServerTest {
  private final long timeout = 1000;
  private NetworkServer server;

  @Before
  public void setup() throws IOException {
    server = new NetworkServer();
    server.bind(9999);
  }

  @After
  public void shutdown() throws Exception {
    server.stop();
    sleep(timeout);
  }

  @Test
  public void serverAcceptsConnections() throws Exception {
    BlockingQueue<NetworkEvent> queue = new LinkedBlockingDeque<>();
    server.addListener(e -> queue.add(e));

    List<Socket> sockets = newArrayList();
    for (int i = 0; i < 1000; i++) {
      Socket socket = new Socket("localhost", 9999);
      sockets.add(socket); // avoids being collected

      NetworkEvent event = queue.poll(1, TimeUnit.SECONDS);
      assertNotNull(event);
      assertEquals(ACCEPT, event.getType());
      assertNotNull(event.getSocketChannel());
    }
  }

  @Test
  public void serverRespondsToClients() throws Exception {
    String sent = "Sample message";
    StringEncoder encoder = new StringEncoder("\n");
    StringDecoder decoder = new StringDecoder("\n");
    server.addListener(new EchoService());

    List<Socket> sockets = newArrayList();
    for (int i = 0; i < 1000; i++) {
      Socket socket = new Socket("localhost", 9999);
      socket.getOutputStream().write(encoder.apply(newArrayList(sent)));
      sockets.add(socket); // avoids being collected

      byte[] bytes = new byte[100];
      int n = socket.getInputStream().read(bytes);
      bytes = new String(bytes, 0, n).getBytes();
      String rcvd = decoder.apply(bytes).get(0);

      assertEquals(sent, rcvd);
    }
  }

  @Test
  public void serverAcceptsClientsClose() throws Exception {
    BlockingQueue<NetworkEvent> queue = new LinkedBlockingDeque<>();
    server.addListener(e -> {
      if (e.getType() == DISCONNECT) {
        queue.add(e);
      }
    });

    for (int i = 0; i < 1000; i++) {
      Socket socket = new Socket("localhost", 9999);
      socket.close();

      NetworkEvent evt = queue.poll(1, TimeUnit.SECONDS);
      assertNotNull(evt);
      assertEquals(DISCONNECT, evt.getType());
      assertNotNull(evt.getSocketChannel());
    }
  }

}
