package network;

import org.apache.log4j.Logger;

import java.io.IOException;
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
        SocketChannel socketChannel = network.accept(serverSocketChannel);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setNetwork(network);
        networkEvent.setType(NetworkEventType.ACCEPT);
        networkEvent.setSocketChannel(socketChannel);

        network.handleEvent(networkEvent);
    }

    public void handleConnect(SelectionKey key) throws IOException {
        logger.info("handling connect...");

        SocketChannel socketChannel = (SocketChannel) key.channel();
        network.connect(socketChannel);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setNetwork(network);
        networkEvent.setType(NetworkEventType.CONNECT);
        networkEvent.setSocketChannel(socketChannel);

        network.handleEvent(networkEvent);
    }

    public void handleRead(SelectionKey key) throws IOException {
        logger.info("handling read...");

        SocketChannel socketChannel = (SocketChannel) key.channel();
        byte[] bytes = network.receiveBytes(socketChannel);

        if (bytes == null) {
            logger.info("disconnected by the client...");

            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setNetwork(network);
            networkEvent.setType(NetworkEventType.DISCONNECT);
            networkEvent.setSocketChannel(socketChannel);

            network.handleEvent(networkEvent);
        } else {
            NetworkEvent networkEvent = new NetworkEvent();
            networkEvent.setNetwork(network);
            networkEvent.setType(NetworkEventType.READ);
            networkEvent.setSocketChannel(socketChannel);
            networkEvent.setData(bytes);

            network.handleEvent(networkEvent);
        }
    }

    public void handleWrite(SelectionKey key) throws IOException {
        logger.info("handling write...");

        SocketChannel socketChannel = (SocketChannel) key.channel();
        network.sendBytes(socketChannel);

        NetworkEvent networkEvent = new NetworkEvent();
        networkEvent.setType(NetworkEventType.WRITE);
        networkEvent.setSocketChannel(socketChannel);
        network.handleEvent(networkEvent);
    }
}
