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

/**
 * Created by Rafael on 1/20/2017.
 */
public class EchoClient {

    private static final Logger logger = Logger.getLogger(EchoClient.class);

    private final String host;
    private final int port;
    private final List<MessageListener> messageListeners = Lists.newCopyOnWriteArrayList();
    private final MessageEncoder<String> encoder = new TokenMessageEncoder("\n");
    private final BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
    private final NonBlockingNetwork network;
    private SocketChannel socketChannel;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.network = new NonBlockingNetwork();
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

    public void send(String message) {
        network.send(socketChannel, encoder.encode(message));
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
        if (event.getType() == NetworkEventType.CONNECT) {
            socketChannel = event.getSocketChannel();
        } else if (event.getType() == NetworkEventType.READ) {
            List<String> messages = encoder.decode(event.getData());
            for (String message : messages) {
                notifyMessageListeners(message);
            }
        }
    }
}
