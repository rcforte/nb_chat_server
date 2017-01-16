package network;

import java.nio.channels.SocketChannel;

/**
 * Created by Rafael on 1/15/2017.
 */
public class NetworkEvent {
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
