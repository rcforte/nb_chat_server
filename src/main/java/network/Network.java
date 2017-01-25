package network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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

  private final List<NetworkListener> listeners = Lists.newCopyOnWriteArrayList();

  protected final SelectorListener selectorListener = new SelectorListener() {
    @Override
    public void onClosed() {
      stopImpl();
    }
    @Override
    public void onBeforeSelect() {
      checkWrites();
    }
  };
  protected final ExecutorService executor = Executors.newSingleThreadExecutor();
  protected final SelectorHandler handler = new SelectorHandler(this);
  protected final Map<SocketChannel, Queue<ByteBuffer>> writeQueues = Maps.newConcurrentMap();
  protected Selector selector;
  protected SelectorLoop selectorLoop;

  public void stop() {
    logger.info("requesting stop...");
    selectorLoop.stop();
  }

  protected void stopImpl() {
    try {
      for (SocketChannel channel : writeQueues.keySet()) {
        channel.close();
      }
    } catch (Exception e) {
      logger.error("Error closing the writeQueues", e);
    }
  }

  public void addListener(NetworkListener lstn) {
    checkArgument(lstn != null, "lstn cannot be null");
    listeners.add(lstn);
  }

  public void removeListener(NetworkListener lstn) {
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

  private void checkWrites() {
    for (Map.Entry<SocketChannel, Queue<ByteBuffer>> entry : writeQueues.entrySet()) {
      Queue<ByteBuffer> queue = entry.getValue();
      if (!queue.isEmpty()) {
        SocketChannel socketChannel = entry.getKey();
        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
      }
    }
  }
}

