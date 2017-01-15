package chat.server;

import chat.common.RequestMessage;
import chat.common.RequestMessageType;
import chat.common.ResponseMessage;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class);

    private final int port;
    private final NonBlockingNetwork network;
    private final NetworkListener networkListener = networkEvent -> handle(networkEvent);

    public ChatServer(int port) {
        this.port = port;
        this.network = new NonBlockingNetwork();
        this.network.addNetworkListener(networkListener);
    }

    public void start() throws IOException {
        network.bind(port);
    }

    public void stop() throws IOException {
        logger.info("stopping server...");
        network.stop();
    }

    void handle(NetworkEvent networkEvent) {
        if (networkEvent.getType() == NetworkEventType.READ) {
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            byte[] data = networkEvent.getData();
            RequestMessage requestMessage = RequestMessage.fromBytes(data);
            if (requestMessage.getRequestMessageType() == RequestMessageType.GET_ROOMS) {
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setCorrelationId(requestMessage.getCorrelationId());
                responseMessage.addRoom("room1");
                responseMessage.addRoom("room2");

                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();
                network.broadcast(jsonBytes);
            }
        }
    }
}
