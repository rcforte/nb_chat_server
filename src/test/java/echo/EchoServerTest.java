package echo;

import network.NetworkClient;
import network.NetworkServer;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoServerTest {

  @Test
  public void echo() throws Exception {
    NetworkServer server = new NetworkServer();
    server.onRead((channel, bytes) -> server.send(channel, bytes));
    server.bind(9999);

    BlockingQueue<String> queue = new LinkedBlockingDeque<>(1);
    NetworkClient client = new NetworkClient();
    client.onConnected(channel -> client.send("test".getBytes()));
    client.onRead(bytes -> queue.add(new String(bytes)));
    client.connect("localhost", 9999);

    assertThat(queue.poll(1, SECONDS), equalTo("test"));

    client.stop();
    server.stop();
  }

  @Test
  public void chat() throws Exception {

  }
}

