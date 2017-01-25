package network;

import java.nio.channels.SocketChannel;

/**
 * Created by Rafael on 1/24/2017.
 */
public interface OnConnected {
  void call(SocketChannel channel);
}
