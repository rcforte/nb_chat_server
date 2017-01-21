package network.echo;

import com.google.common.collect.Lists;
import network.*;

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
    private final String host;
    private final int port;
    private NonBlockingNetwork network;
    private final List<MessageListener> messageListeners = Lists.newCopyOnWriteArrayList();
    private final TokenMessageEncoder encoder = new TokenMessageEncoder("\n");
    private SocketChannel socketChannel;
    private final BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void connect() throws IOException {
        network = new NonBlockingNetwork();
        network.addNetworkListener(event -> {
            if (event.getType() == NetworkEventType.CONNECT) {
                socketChannel = event.getSocketChannel();
            } else if (event.getType() == NetworkEventType.READ) {
                byte[] bytes = event.getData();
                List<String> messages = encoder.decode(bytes);
                for (String message : messages) {
                    notifyMessageListeners(message);
                }
            }
        });

        network.connect(this.host, this.port);
        try {
            blockingQueue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        byte[] bytes = encoder.encode(message);
        network.send(socketChannel, bytes);
    }

    private void notifyMessageListeners(String message) {
        for (MessageListener messageListener : messageListeners) {
            messageListener.onMessage(message);
        }
    }

    public void disconnect() throws IOException {
        network.stop();
    }
}
