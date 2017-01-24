package network.echo;

import network.Network;
import network.StringDecoder;

import java.io.IOException;

/**
 * Created by Rafael on 1/20/2017.
 */
public class EchoServer {
  private final int port;
  private final Network network;

  public EchoServer(int port) {
    this.port = port;
    this.network = new Network();
    this.network.addNetworkListener(new EchoService());
  }

  public void start() throws IOException {
    network.bind(this.port);
  }

  public void stop() throws IOException {
    network.stop();
  }
}
