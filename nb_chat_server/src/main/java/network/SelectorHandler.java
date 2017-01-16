package network;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by Rafael on 1/15/2017.
 */
class SelectorHandler implements Handler {
    private static final Logger logger = Logger.getLogger(SelectorHandler.class);
    private final NonBlockingNetwork network;

    public SelectorHandler(NonBlockingNetwork network) {
        this.network = network;
    }

    public void handleAccept(SelectionKey key) throws IOException {
        logger.info("handling accept...");

        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(key.selector(), SelectionKey.OP_READ);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(NetworkEventType.ACCEPT);
        networkEvent.setSocketChannel(socketChannel);

        network.handleEvent(networkEvent);
    }

    public void handleConnect(SelectionKey key) throws IOException {
        logger.info("handling connect...");

        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.finishConnect();
        key.interestOps(SelectionKey.OP_READ);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(NetworkEventType.CONNECT);
        networkEvent.setSocketChannel(socketChannel);

        network.handleEvent(networkEvent);
    }

    public void handleRead(SelectionKey key) throws IOException {
        logger.info("handling read...");

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
            logger.info("disconnected by the client...");
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setType(NetworkEventType.DISCONNECT);
            networkEvent.setSocketChannel(socketChannel);
            network.handleEvent(networkEvent);
        } else {
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setType(NetworkEventType.READ);
            networkEvent.setSocketChannel(socketChannel);
            networkEvent.setData(requestBytes.toByteArray());
            network.handleEvent(networkEvent);
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        logger.info("handling write...");

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(NetworkEventType.WRITE);
        networkEvent.setSocketChannel((SocketChannel) key.channel());
        network.handleEvent(networkEvent);
    }
}
