package network.echo;

import network.NetworkClient;
import network.NetworkServer;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoServerTest {

  @Test
  public void echo() throws Exception {
    NetworkServer srv = new NetworkServer();
    srv.onRead((channel, bytes) -> srv.send(channel, bytes));
    srv.bind(9999);

    BlockingQueue<String> queue = new LinkedBlockingDeque<>(1);
    NetworkClient cli = new NetworkClient();
    cli.onConnected(channel -> cli.send("test".getBytes()));
    cli.onRead(bytes -> queue.add(new String(bytes)));
    cli.connect("localhost", 9999);

    assertThat(queue.poll(1, SECONDS), equalTo("test"));

    cli.stop();
    srv.stop();
  }
}

