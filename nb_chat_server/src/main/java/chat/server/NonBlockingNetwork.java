package chat.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static chat.server.NetworkEventType.*;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * non blocking server
 */
public class NonBlockingNetwork {
    private static final Logger logger = Logger.getLogger(NonBlockingNetwork.class);

    private final Map<SocketChannel, Queue<ByteBuffer>> writeQueues = Maps.newConcurrentMap();
    private final SelectHandler handler = new SelectHandler(this);
    private final List<NetworkListener> networkListeners = Lists.newCopyOnWriteArrayList();
    private volatile boolean stopped;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public void bind(int port) throws IOException {
        logger.info("starting server...");

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        logger.info("server started on port " + port);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            startSelect();
        });
    }

    public void connect(String host, int port) throws IOException {
        logger.info("connecting to server...");

        selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(host, port));

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            startSelect();
        });
    }

    public void stop() {
        logger.info("requesting stop...");
        this.stopped = true;
        this.selector.wakeup();
    }

    public void addNetworkListener(NetworkListener networkListener) {
        networkListeners.add(networkListener);
    }

    public void removeNetworkListener(NetworkListener networkListener) {
        networkListeners.remove(networkListener);
    }

    public void send(SocketChannel socketChannel, byte[] data) {
        checkArgument(socketChannel != null, "socketChannel cannot be null");
        checkArgument(data != null, "data cannot be null");

        Queue<ByteBuffer> queue = writeQueues.get(socketChannel);
        queue.add(ByteBuffer.wrap(data));
        selector.wakeup();
    }

    public void broadcast(byte[] data) {
        for (SocketChannel socketChannel : writeQueues.keySet()) {
            send(socketChannel, data);
        }
    }

    void startSelect() {
        while (!stopped) {
            try {
                select();
            } catch (IOException e) {
                logger.error("error during select operation", e);
            }
        }

        logger.info("stopping network...");
        try {
            selector.close();
        } catch (IOException e) {
            logger.error("Error closing selector", e);
        }
        try {
            for (SocketChannel socketChannel : writeQueues.keySet()) {
                socketChannel.close();
            }
        } catch (Exception e) {
            logger.error("Error closing the writeQueues", e);
        }
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (Exception e) {
                logger.error("Error closing server channel", e);
            }
        }
    }

    void select() throws IOException {
        for (Map.Entry<SocketChannel, Queue<ByteBuffer>> entry : writeQueues.entrySet()) {
            Queue<ByteBuffer> queue = entry.getValue();
            if (!queue.isEmpty()) {
                SocketChannel socketChannel = entry.getKey();
                socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            }
        }

        int n = selector.select(1000L);
        if (n == 0) {
            return;
        }

        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();

            if (key.isValid() && key.isAcceptable()) {
                handler.handleAccept(key);
            } else if (key.isValid() && key.isConnectable()) {
                handler.handleConnect(key);
            } else if (key.isValid() && key.isReadable()) {
                handler.handleRead(key);
            } else if (key.isValid() && key.isWritable()) {
                handler.handleWrite(key);
            }

            it.remove();
        }
    }

    void handleEvent(NetworkEvent networkEvent) throws IOException {
        if (networkEvent.getType() == ACCEPT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            writeQueues.put(socketChannel, Queues.newArrayDeque());
        } else if (networkEvent.getType() == CONNECT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            writeQueues.put(socketChannel, Queues.newArrayDeque());
        } else if (networkEvent.getType() == NetworkEventType.DISCONNECT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            socketChannel.close();
            SelectionKey selectionKey = socketChannel.keyFor(selector);
            selectionKey.cancel();
            writeQueues.remove(socketChannel);
        } else if (networkEvent.getType() == NetworkEventType.READ) {
            //
            // Do nothing
            //
        } else if (networkEvent.getType() == WRITE) {
            // Get write queue for channel
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            Queue<ByteBuffer> queue = writeQueues.get(socketChannel);
            // Write first buffer in the queue
            ByteBuffer byteBuffer = queue.peek();
            socketChannel.write(byteBuffer);
            if (byteBuffer.hasRemaining()) {
                // still has bytes, compact buffer for next time
                byteBuffer.compact();
            } else {
                // all bytes written, get rid of buffer
                queue.remove();
            }
            if (queue.isEmpty()) {
                // nothing else to write, go back to read mode
                socketChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
            }
        }

        notifyListeners(networkEvent);
    }

    void notifyListeners(NetworkEvent networkEvent) {
        for (NetworkListener networkListener : networkListeners) {
            networkListener.onEvent(networkEvent);
        }
    }

}

