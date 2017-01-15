package chat.server;

import chat.message.Chat.RequestMessage;
import chat.message.Chat.RequestMessage.Type;
import chat.message.Chat.ResponseMessage;
import chat.server.chat.ChatUser;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static chat.server.NetworkEventType.ACCEPT;
import static chat.server.NetworkEventType.WRITE;

/**
 * non blocking server
 */
public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class);

    private final int port;
    private final Map<SocketChannel, ChatUser> clients = new HashMap<>();
    private final Map<SocketChannel, Queue<ByteBuffer>> writeQueues = Maps.newHashMap();
    private final Queue<WriteTicket> outputQueue = new LinkedList<>();
    private final SelectHandler handler = new SelectHandler(this);

    private volatile boolean stopped = false;

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public ChatServer(int port) throws Exception {
        this.port = port;
    }

    /**
     * performs NIO plumbing
     */
    public void start() throws Exception {
        logger.info("starting server...");

        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        logger.info("server started on port " + port);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            startSelect();
        });
    }

    void startSelect() {
        while (!stopped) {
            try {
                select();
            } catch (IOException e) {
                logger.error("error during select operation", e);
            }
        }

        logger.info("stopping server...");
        try {
            selector.close();
        } catch (IOException e) {
            logger.error("Error closing selector", e);
        }
        try {
            for (SocketChannel socketChannel : clients.keySet()) {
                socketChannel.close();
            }
        } catch (Exception e) {
            logger.error("Error closing the clients", e);
        }
        try {
            serverSocketChannel.close();
        } catch (Exception e) {
            logger.error("Error closing server channel", e);
        }
    }

    void select() throws IOException {
        if (!outputQueue.isEmpty()) {
            for (WriteTicket ticket : outputQueue) {
                ticket.setWriteInterest(selector);
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
            } else if (key.isValid() && key.isReadable()) {
                handler.handleRead(key);
            } else if (key.isValid() && key.isWritable()) {
                handler.handleWrite(key);
            }

            it.remove();
        }
    }

    public void stop() {
        logger.info("requesting server stop...");
        this.stopped = true;
        this.selector.wakeup();
    }


    public List<SocketChannel> getUserChannels() {
        return new ArrayList<SocketChannel>(clients.keySet());
    }

    public ChatUser getUserByChannel(SocketChannel channel) {
        return clients.get(channel);
    }

    void handleEvent(NetworkEvent networkEvent) throws IOException {
        if (networkEvent.getType() == ACCEPT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            clients.put(socketChannel, new ChatUser());
        } else if (networkEvent.getType() == NetworkEventType.DISCONNECT) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            socketChannel.close();
            SelectionKey selectionKey = socketChannel.keyFor(selector);
            selectionKey.cancel();
            clients.remove(socketChannel);
        } else if (networkEvent.getType() == NetworkEventType.READ) {
            try {
                SocketChannel socketChannel = networkEvent.getSocketChannel();
                byte[] data = networkEvent.getData();
                RequestMessage request = RequestMessage.parseFrom(data);
                if (request.getType() == Type.GET_ROOMS) {
                    logger.info("handling get rooms request...");
                    ResponseMessage response = ResponseMessage.newBuilder()
                            .addChatRoom("room1")
                            .addChatRoom("room2")
                            .build();
                    outputQueue.offer(new WriteTicket(socketChannel, ByteBuffer.wrap(response.toByteArray())));
                    selector.wakeup();
                }
            } catch (InvalidProtocolBufferException e) {
                logger.error("Error reading from client", e);
            }
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
                queue.poll();
            }
            if (queue.isEmpty()) {
                // nothing else to write, go back to read mode
                socketChannel.keyFor(selector).interestOps(SelectionKey.OP_READ);
            }
        }
    }
}

class SelectHandler {
    private static final Logger logger = Logger.getLogger(Handler.class);
    private final ChatServer chatServer;

    public SelectHandler(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    public void handleAccept(SelectionKey key) throws IOException {
        logger.info("handling accept...");
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(key.selector(), SelectionKey.OP_READ);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(ACCEPT);
        networkEvent.setSocketChannel(socketChannel);

        chatServer.handleEvent(networkEvent);
    }

    public void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
        int n = 0;

        try {
            while ((n = socketChannel.read(byteBuffer)) > 0) {
                byte[] bytes = new byte[n];
                byteBuffer.flip();
                byteBuffer.get(bytes);
                byteBuffer.compact();
                requestBytes.write(bytes);
            }
        } catch (IOException e) {
            n = -1;
        }

        if (n == -1) {
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setType(NetworkEventType.DISCONNECT);
            networkEvent.setSocketChannel(socketChannel);
            chatServer.handleEvent(networkEvent);
        } else {
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setType(NetworkEventType.READ);
            networkEvent.setSocketChannel(socketChannel);
            networkEvent.setData(requestBytes.toByteArray());
            chatServer.handleEvent(networkEvent);
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(WRITE);
        networkEvent.setSocketChannel((SocketChannel) key.channel());
        chatServer.handleEvent(networkEvent);
    }
}

class NetworkEvent {
    private NetworkEventType type;
    private SocketChannel socketChannel;
    private byte[] data;

    public NetworkEventType getType() {
        return type;
    }

    public void setType(NetworkEventType type) {
        this.type = type;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

enum NetworkEventType {
    DISCONNECT, READ, WRITE, ACCEPT;
}
