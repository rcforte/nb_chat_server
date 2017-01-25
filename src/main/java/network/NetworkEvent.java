package network;

import java.nio.channels.SocketChannel;

/**
 * Created by Rafael on 1/15/2017.
 */
public class NetworkEvent {
  private Network network;
  private NetworkEventType type;
  private SocketChannel socketChannel;
  private byte[] data;

  public Network getNetwork() {
    return network;
  }

  public void setNetwork(Network network) {
    this.network = network;
  }

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

  public NetworkEventType type() {
    return getType();
  }

  public byte[] data() {
    return getData();
  }

  public SocketChannel channel() {
    return getSocketChannel();
  }
}
