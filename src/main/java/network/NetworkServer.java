package network;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkArgument;
import static network.NetworkEventType.READ;

/**
 * Created by Rafael on 1/24/2017.
 */
public class NetworkServer extends Network {
  private static final Logger logger = Logger.getLogger(NetworkServer.class);

  private ServerSocketChannel channel;
  private BiConsumer onRead;

  public NetworkServer() {
    addListener(evt -> {
      if (evt.type() == READ && onRead != null) {
        onRead.accept(evt.channel(), evt.data());
      }
    });
  }

  public void bind(int port) throws IOException {
    checkArgument(port >= 0, "port cannot be negative");

    logger.info("binding to port " + port);

    selector = Selector.open();
    selectorLoop = new SelectorLoop(selector, selectorListener, handler);
    channel = ServerSocketChannel.open();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_ACCEPT);
    channel.socket().bind(new InetSocketAddress(port));

    logger.info("socket bound to port " + port);

    executor.execute(() -> selectorLoop.start());
  }

  public void onRead(BiConsumer<SocketChannel, byte[]> func) {
    this.onRead = func;
  }

  @Override
  protected void stopImpl(){
    super.stopImpl();
    if (channel != null) {
      try {
        channel.close();
      } catch (Exception e) {
        logger.error("Error closing server channel", e);
      }
    }
  }
}
