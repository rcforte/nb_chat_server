package network.echo;

import network.*;

import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * Created by Rafael on 1/16/2017.
 */
public class EchoService implements NetworkListener {
  private final Encoder<String> encoder;

  public EchoService(Encoder<String> encoder) {
    this.encoder = encoder;
  }

  @Override
  public void onEvent(NetworkEvent networkEvent) {
    if (networkEvent.getType() == NetworkEventType.READ) {
      SocketChannel socketChannel = networkEvent.getSocketChannel();
      byte[] data = networkEvent.getData();
      List<String> messages = encoder.decode(data);

      Network network = networkEvent.getNetwork();
      messages.stream().forEach(message ->
          network.send(socketChannel, encoder.encode(message))
      );
    }
  }
}
