package network;

import org.apache.log4j.Logger;
import sun.security.krb5.internal.NetClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static network.NetworkEventType.CONNECT;
import static network.NetworkEventType.READ;

/**
 * Created by Rafael on 1/24/2017.
 */
public class NetworkClient extends Network {
  private static final Logger logger = Logger.getLogger(NetClient.class);

  private SocketChannel channel;
  private Consumer<SocketChannel> onConnected;
  private Consumer<byte[]> onRead;

  public NetworkClient() {
    addListener(evt -> {
      if (evt.type() == CONNECT && onConnected != null) {
        onConnected.accept(evt.channel());
      } else if (evt.type() == READ && onRead != null) {
        onRead.accept(evt.data());
      }
    });
  }

  public void connect(String host, int port) throws IOException {
    checkArgument(host != null, "host cannot be null");
    checkArgument(port >= 0, "port cannot be negative");

    logger.info("connecting to server...");

    selector = Selector.open();
    selectorLoop = new SelectorLoop(selector, selectorListener, handler);
    channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_CONNECT);
    channel.connect(new InetSocketAddress(host, port));

    logger.info("connected to " + host + ":" + port);

    executor.execute(() -> selectorLoop.start());
  }

  public void send(byte[] data) {
    send(channel, data);
  }

  @Override
  protected void stopImpl() {
    if (channel != null) {
      try {
        channel.close();
      } catch (Exception e) {
        logger.error("Error closing channel", e);
      }
    }
  }

  public void onConnected(Consumer<SocketChannel> func) {
    this.onConnected = func;
  }

  public void onRead(Consumer<byte[]> func) {
    this.onRead = func;
  }
}

