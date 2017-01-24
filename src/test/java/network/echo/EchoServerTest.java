package network.echo;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoServerTest {

  @Test
  public void test() throws Exception {
    BlockingQueue<String> queue = new LinkedBlockingDeque<>();

    EchoServer server = new EchoServer(9999);
    server.start();

    EchoClient client = new EchoClient("localhost", 9999);
    client.addMessageListener(message -> queue.add(message));
    client.connect();
    client.send("Test");

    assertEquals("Test", queue.poll(2, TimeUnit.SECONDS));

    client.disconnect();
    server.stop();
  }
}

