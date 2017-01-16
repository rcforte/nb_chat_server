package network;

import java.nio.channels.SocketChannel;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoNetworkListener implements NetworkListener {

    private final NonBlockingNetwork network;

    public EchoNetworkListener(NonBlockingNetwork network) {
        this.network = network;
    }

    @Override
    public void onEvent(NetworkEvent networkEvent) {
        if (networkEvent.getType() == NetworkEventType.READ) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            byte[] data = networkEvent.getData();
            network.send(socketChannel, data);
        }
    }
}
