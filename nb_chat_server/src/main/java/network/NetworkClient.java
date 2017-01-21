package network;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Rafael on 1/20/2017.
 */
class NetworkClient {
    private final List<NetworkListener> networkListeners = Lists.newCopyOnWriteArrayList();
    private final NonBlockingNetwork network;
    private final NetworkListener networkListener = networkEvent -> {
        if (networkEvent.getType() == NetworkEventType.CONNECT) {
            socketChannel = networkEvent.getSocketChannel();
        }
    };

    private SocketChannel socketChannel;

    public NetworkClient(NonBlockingNetwork network) {
        this.network = network;
        this.network.addNetworkListener(networkListener);
    }

    public void addNetworkListener(NetworkListener networkListener) {
        this.network.addNetworkListener(networkListener);
    }

    public void removeNetworkListener(NetworkListener networkListener) {
        this.network.removeNetworkListener(networkListener);
    }

    public void connect(String host, int port) throws IOException {
        this.network.connect(host, port);
    }

    public void send(byte[] data) {
        checkArgument(data != null, "data cannot be null");
        checkArgument(data.length > 0, "data cannot be empty");
        checkNotNull(socketChannel, "client not connected");

        this.network.send(socketChannel, data);
    }
}
