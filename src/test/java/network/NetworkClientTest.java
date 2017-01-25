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

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.sleep;
import static network.NetworkEventType.READ;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Rafael on 1/16/2017.
 */
public class NetworkClientTest {
  NetworkServer server;

  @Before
  public void setup() throws IOException {
    EchoService echoService = new EchoService();
    server = new NetworkServer();
    server.addListener(echoService);
    server.bind(9999);
  }

  @After
  public void teardown() throws Exception {
    server.stop();
    sleep(1000);
  }

  @Test
  public void receivesResponseUsingListener() throws Exception {
    BlockingQueue<NetworkEvent> queue = new LinkedBlockingDeque<>();
    StringEncoder encoder = new StringEncoder("\n");
    StringDecoder decoder = new StringDecoder("\n");
    NetworkClient client = new NetworkClient();
    client.addListener(evt -> {
      if (evt.getType() == READ) {
        queue.add(evt);
      }
    });
    client.connect("localhost", 9999);
    sleep(1000);

    client.send(encoder.apply(newArrayList("This is a test")));

    NetworkEvent event = queue.poll(1, TimeUnit.SECONDS);
    assertNotNull(event);
    assertEquals(READ, event.getType());
    assertEquals("This is a test", decoder.apply(event.getData()).get(0));
  }

  @Test
  public void receivesMessage() throws Exception {
    BlockingQueue<NetworkEvent> queue = new LinkedBlockingDeque<>();
    NetworkListener lstn = networkEvent -> {
      if (networkEvent.getType() == READ) {
        queue.add(networkEvent);
      }
    };
    StringEncoder encoder = new StringEncoder("\n");
    StringDecoder decoder = new StringDecoder("\n");
    List<String> sent = newArrayList("Message1", "Message2", "Message3");

    NetworkClient cli = new NetworkClient();
    cli.addListener(lstn);
    cli.connect("localhost", 9999);
    sleep(1000);

    sent.stream()
        .map(Lists::newArrayList)
        .forEach(msgs -> server.broadcast(encoder.apply(msgs)));

    List<String> received = newArrayList();
    while (received.size() != 3) {
      NetworkEvent evt = queue.poll(1, TimeUnit.SECONDS);
      assertNotNull(evt);
      assertEquals(READ, evt.getType());
      received.addAll(decoder.apply(evt.getData()));
    }

    assertEquals("Wrong messages received from server", sent, received);
  }
}

