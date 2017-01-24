package network.echo;

import com.google.common.collect.Lists;
import network.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static network.NetworkEventType.CONNECT;
import static network.NetworkEventType.READ;

/**
 * Created by Rafael on 1/20/2017.
 */
public class EchoClient {

  private static final Logger logger = Logger.getLogger(EchoClient.class);

  private final String host;
  private final int port;
  private final List<MessageListener> messageListeners = Lists.newCopyOnWriteArrayList();
  private final BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
  private final Network network;
  private final StringEncoder encoder = new StringEncoder("\n");
  private final StringDecoder decoder = new StringDecoder("\n");
  private SocketChannel channel;

  public EchoClient(String host, int port) {
    this.host = host;
    this.port = port;
    this.network = new Network();
    this.network.addNetworkListener(event -> handle(event));
  }

  public void addMessageListener(MessageListener messageListener) {
    messageListeners.add(messageListener);
  }

  public void connect() throws IOException {
    network.connect(this.host, this.port);
    try {
      blockingQueue.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void send(String msg) {
    network.send(channel, encoder.apply(newArrayList(msg)));
  }

  private void notifyMessageListeners(String message) {
    for (MessageListener messageListener : messageListeners) {
      messageListener.onMessage(message);
    }
  }

  public void disconnect() throws IOException {
    network.stop();
  }

  void handle(NetworkEvent event) {
    if (event.getType() == CONNECT) {
      channel = event.getSocketChannel();
    } else if (event.getType() == READ) {
      decoder.apply(event.getData()).stream()
          .forEach(this::notifyMessageListeners);
    }
  }
}
