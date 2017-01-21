package network.echo;

import network.NonBlockingNetwork;
import network.TokenMessageEncoder;

import java.io.IOException;

/**
 * Created by Rafael on 1/20/2017.
 */
public class EchoServer {
    private final int port;
    private final NonBlockingNetwork network;

    public EchoServer(int port) {
        this.port = port;
        this.network = new NonBlockingNetwork();
        this.network.addNetworkListener(
                new EchoService(
                        new TokenMessageEncoder("\n")));
    }

    public void start() throws IOException {
        network.bind(this.port);
    }

    public void stop() throws IOException {
        network.stop();
    }
}
