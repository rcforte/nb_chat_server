package network.echo;

import network.*;

import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoService implements NetworkListener {
  private final StringEncoder encoder = new StringEncoder("\n");
  private final StringDecoder decoder = new StringDecoder("\n");

  @Override
  public void onEvent(NetworkEvent event) {
    if (event.getType() == NetworkEventType.READ) {
      SocketChannel channel = event.getSocketChannel();
      byte[] data = event.getData();
      List<String> messages = decoder.apply(data);
      Network network = event.getNetwork();
      network.send(channel, encoder.apply(messages));
    }
  }
}
