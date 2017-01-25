package network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Queues.newArrayDeque;
import static network.NetworkEventType.*;

/**
 * non blocking server
 */
public class Network {
  private static final Logger logger = Logger.getLogger(Network.class);

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final List<NetworkListener> listeners = Lists.newCopyOnWriteArrayList();
  private final Map<SocketChannel, Queue<ByteBuffer>> writeQueues = Maps.newConcurrentMap();
  private final SelectorHandler handler = new SelectorHandler(this);
  private final SelectorListener selectorListener = new SelectorListener() {
    @Override
    public void onClosed() {
      try {
        for (SocketChannel channel : writeQueues.keySet()) {
          channel.close();
        }
      } catch (Exception e) {
        logger.error("Error closing the writeQueues", e);
      }

      if (serverChannel != null) {
        try {
          serverChannel.close();
        } catch (Exception e) {
          logger.error("Error closing server channel", e);
        }
      }
    }

    @Override
    public void onBeforeSelect() {
      for (Map.Entry<SocketChannel, Queue<ByteBuffer>> entry : writeQueues.entrySet()) {
        Queue<ByteBuffer> queue = entry.getValue();
        if (!queue.isEmpty()) {
          SocketChannel socketChannel = entry.getKey();
          socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        }
      }
    }
  };

  private Selector selector;
  private SelectorLoop selectorLoop;
  private ServerSocketChannel serverChannel;

  public void bind(int port) throws IOException {
    checkArgument(port >= 0, "port cannot be negative");

    logger.info("binding to port " + port);
    selector = Selector.open();
    selectorLoop = new SelectorLoop(selector, selectorListener, handler);
    serverChannel = ServerSocketChannel.open();
    serverChannel.configureBlocking(false);
    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    serverChannel.socket().bind(new InetSocketAddress(port));
    logger.info("socket bound to port " + port);

    executor.execute(() -> selectorLoop.start());
  }

  public void connect(String host, int port) throws IOException {
    checkArgument(host != null, "host cannot be null");
    checkArgument(port >= 0, "port cannot be negative");

    logger.info("connecting to server...");
    selector = Selector.open();
    selectorLoop = new SelectorLoop(selector, selectorListener, handler);
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_CONNECT);
    channel.connect(new InetSocketAddress(host, port));
    logger.info("connected to " + host + ":" + port);

    executor.execute(() -> selectorLoop.start());
  }

  public void stop() {
    logger.info("requesting stop...");
    selectorLoop.stop();
  }

  public void addNetworkListener(NetworkListener lstn) {
    checkArgument(lstn != null, "lstn cannot be null");
    listeners.add(lstn);
  }

  public void removeNetworkListener(NetworkListener lstn) {
    checkArgument(lstn != null, "lstn cannot be null");
    listeners.remove(lstn);
  }

  void notifyListeners(NetworkEvent event) {
    checkArgument(event != null, "event cannot be null");
    for (NetworkListener lstn : listeners) {
      lstn.onEvent(event);
    }
  }

  public void send(SocketChannel channel, byte[] data) {
    checkArgument(channel != null, "channel cannot be null");
    checkArgument(data != null, "data cannot be null");
    Queue<ByteBuffer> queue = writeQueues.get(channel);
    queue.add(ByteBuffer.wrap(data));
  }

  public void broadcast(byte[] data) {
    checkArgument(data != null, "data cannot be null");
    checkArgument(data.length > 0, "data cannot be empty");
    writeQueues.keySet().stream().forEach(channel -> send(channel, data));
  }

  void addSocket(SocketChannel channel) {
    writeQueues.put(channel, newArrayDeque());
  }

  void handleEvent(NetworkEvent event) throws IOException {
    checkArgument(event != null, "event cannot be null");

    if (event.type() == ACCEPT) {
      addSocket(event.channel());
    } else if (event.type() == CONNECT) {
      addSocket(event.channel());
    } else if (event.type() == DISCONNECT) {
      disconnect(event.channel());
    } else if (event.type() == READ) {
      // Do nothing
    } else if (event.getType() == NetworkEventType.WRITE) {
      // Do nothing
    }

    notifyListeners(event);
  }

  public void disconnect(SocketChannel channel) throws IOException {
    channel.keyFor(selector).cancel();
    channel.close();
    writeQueues.remove(channel);
  }

  public void sendBytes(SocketChannel channel) throws IOException {
    Queue<ByteBuffer> queue = writeQueues.get(channel);
    ByteBuffer buffer = queue.peek();
    channel.write(buffer);
    if (buffer.hasRemaining()) {
      buffer.compact();
    } else {
      queue.remove();
    }
    if (queue.isEmpty()) {
      channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
    }
  }

  public byte[] receiveBytes(SocketChannel channel) {
    int n = 0;
    byte[] result = null;

    try {
      ByteBuffer buffer = ByteBuffer.allocate(1024);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      while ((n = channel.read(buffer)) > 0) {
        byte[] bytes = new byte[n];
        buffer.flip();
        buffer.get(bytes);
        buffer.compact();
        stream.write(bytes);
      }
      result = stream.toByteArray();
    } catch (IOException e) {
      n = -1;
    }

    if (n == -1) {
      result = null;
    }

    return result;
  }

  public SocketChannel accept(ServerSocketChannel srvChnl) throws IOException {
    SocketChannel channel = srvChnl.accept();
    channel.configureBlocking(false);
    channel.register(selector, SelectionKey.OP_READ);
    return channel;
  }

  public void connect(SocketChannel channel) throws IOException {
    channel.finishConnect();
    channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
  }
}

