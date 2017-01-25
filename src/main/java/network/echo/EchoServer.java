package network.echo;

import network.NetworkServer;

import java.io.IOException;

/**
 * Created by Rafael on 1/20/2017.
 */
public class EchoServer {
  private final int port;
  private final NetworkServer server;

  public EchoServer(int port) {
    this.port = port;
    this.server = new NetworkServer();
    this.server.addListener(new EchoService());
  }

  public void start() throws IOException {
    server.bind(this.port);
  }

  public void stop() throws IOException {
    server.stop();
  }
}
