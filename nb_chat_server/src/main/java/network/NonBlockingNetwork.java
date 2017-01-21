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

/**
 * non blocking server
 */
public class NonBlockingNetwork {
    private static final Logger logger = Logger.getLogger(NonBlockingNetwork.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<NetworkListener> networkListeners = Lists.newCopyOnWriteArrayList();
    private final Map<SocketChannel, Queue<ByteBuffer>> writeQueues = Maps.newConcurrentMap();
    private final SelectorHandler handler = new SelectorHandler(this);
    private final SelectorListener selectorListener = new SelectorListener() {
        @Override
        public void onClosed() {
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
    private ServerSocketChannel serverSocketChannel;

    public void bind(int port) throws IOException {
        checkArgument(port >= 0, "port cannot be negative");

        logger.info("starting server...");

        selector = Selector.open();
        selectorLoop = new SelectorLoop(selector, selectorListener, handler);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        logger.info("server started on port " + port);

        executorService.execute(() -> {
            selectorLoop.start();
        });
    }

    public void connect(String host, int port) throws IOException {
        checkArgument(host != null, "host cannot be null");
        checkArgument(port >= 0, "port cannot be negative");

        logger.info("connecting to server...");

        selector = Selector.open();
        selectorLoop = new SelectorLoop(selector, selectorListener, handler);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(host, port));

        logger.info("connected to " + host + ":" + port);

        executorService.execute(() -> {
            selectorLoop.start();
        });
    }

    public void stop() {
        logger.info("requesting stop...");
        selectorLoop.stop();
    }

    public void addNetworkListener(NetworkListener networkListener) {
        checkArgument(networkListener != null, "networkListener cannot be null");
        networkListeners.add(networkListener);
    }

    public void removeNetworkListener(NetworkListener networkListener) {
        checkArgument(networkListener != null, "networkListener cannot be null");
        networkListeners.remove(networkListener);
    }

    void notifyListeners(NetworkEvent networkEvent) {
        checkArgument(networkEvent != null, "networkEvent cannot be null");

        for (NetworkListener networkListener : networkListeners) {
            networkListener.onEvent(networkEvent);
        }
    }

    public void send(SocketChannel socketChannel, byte[] data) {
        checkArgument(socketChannel != null, "socketChannel cannot be null");
        checkArgument(data != null, "data cannot be null");

        Queue<ByteBuffer> queue = writeQueues.get(socketChannel);
        queue.add(ByteBuffer.wrap(data));
    }

    public void broadcast(byte[] data) {
        checkArgument(data != null, "data cannot be null");
        checkArgument(data.length > 0, "data cannot be empty");

        for (SocketChannel socketChannel : writeQueues.keySet()) {
            send(socketChannel, data);
        }
    }

    void addSocket(SocketChannel socketChannel) {
        writeQueues.put(socketChannel, Queues.newArrayDeque());
    }

    void handleEvent(NetworkEvent networkEvent) throws IOException {
        checkArgument(networkEvent != null, "networkEvent cannot be null");

        if (networkEvent.getType() == NetworkEventType.ACCEPT) {
            addSocket(networkEvent.getSocketChannel());
        } else if (networkEvent.getType() == NetworkEventType.CONNECT) {
            addSocket(networkEvent.getSocketChannel());
        } else if (networkEvent.getType() == NetworkEventType.DISCONNECT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            SelectionKey selectionKey = socketChannel.keyFor(selector);
            selectionKey.cancel();
            socketChannel.close();
            writeQueues.remove(socketChannel);
        } else if (networkEvent.getType() == NetworkEventType.READ) {
            //
            // Do nothing
            //
        } else if (networkEvent.getType() == NetworkEventType.WRITE) {
            //
            // Do nothing
            //
        }

        notifyListeners(networkEvent);
    }

    public void sendBytes(SocketChannel socketChannel) throws IOException {
        Queue<ByteBuffer> queue = writeQueues.get(socketChannel);
        ByteBuffer byteBuffer = queue.peek();
        socketChannel.write(byteBuffer);
        if (byteBuffer.hasRemaining()) {
            byteBuffer.compact();
        } else {
            queue.remove();
        }
        if (queue.isEmpty()) {
            socketChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
        }
    }

    public byte[] receiveBytes(SocketChannel socketChannel) {
        int n = 0;
        byte[] result = null;

        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            while ((n = socketChannel.read(byteBuffer)) > 0) {
                byte[] bytes = new byte[n];
                byteBuffer.flip();
                byteBuffer.get(bytes);
                byteBuffer.compact();
                byteStream.write(bytes);
            }
            result = byteStream.toByteArray();
        } catch (IOException e) {
            n = -1;
        }

        if (n == -1) {
            result = null;
        }

        return result;
    }

    public SocketChannel accept(ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        return socketChannel;
    }

    public void connect(SocketChannel socketChannel) throws IOException {
        socketChannel.finishConnect();
        socketChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
    }
}

