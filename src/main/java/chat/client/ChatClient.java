package chat.client;

import chat.common.RequestMessage;
import chat.common.RequestMessageType;
import chat.common.ResponseMessage;
import network.*;
import com.google.common.collect.Maps;
import network.chat.ChatRoom;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static chat.common.RequestMessageType.GET_ROOMS;
import static chat.common.RequestMessageType.GET_ROOM_USERS;
import static com.google.common.base.Preconditions.checkArgument;

public class ChatClient {

    private static final Logger logger = Logger.getLogger(ChatClient.class);

    private final String host;
    private final int port;
    private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
    private final NonBlockingNetwork network;
    private final NetworkListener networkListener = networkEvent -> handle(networkEvent);
    private final EventWaiter eventWaiter = new EventWaiter();
    private final Map<String, Processor> processors = Maps.newConcurrentMap();
    private final Map<NetworkEventType, BlockingQueue<NetworkEvent>> events = Maps.newHashMap();
    private final int timeout = 5;
    private SocketChannel socketChannel;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.network = new NonBlockingNetwork();
        this.network.addNetworkListener(networkListener);
        //this.network.addNetworkListener(eventWaiter);

        for (NetworkEventType networkEventType : NetworkEventType.values()) {
            events.put(networkEventType, new LinkedBlockingDeque<>());
        }
    }

    public void addChatListener(ChatListener chatListener) {
        chatListeners.add(chatListener);
    }

    public void removeChatListener(ChatListener chatListener) {
        chatListeners.remove(chatListener);
    }

    public void connect() throws IOException {
        network.connect(host, port);
        waitForEvent(NetworkEventType.CONNECT);
    }

    public void stop() {
        logger.info("stopping chat.client...");
        network.stop();
        network.removeNetworkListener(networkListener);
    }

    public void getChatRooms(Processor processor) {
        logger.info("sending get rooms requestMessage");

        String correlationId = UUID.randomUUID().toString();

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(GET_ROOMS);
        requestMessage.setCorrelationId(correlationId);

        processors.put(correlationId, processor);

        network.send(this.socketChannel, requestMessage.toJson().getBytes());
    }

    public void getChatRoomUsers(String chatRoom, Processor processor) {
        logger.info("sending get rooms requestMessage");

        Map<String, String> payload = Maps.newHashMap();
        payload.put("chatRoom", chatRoom);

        String correlationId = UUID.randomUUID().toString();

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(GET_ROOM_USERS);
        requestMessage.setPayload(payload);
        requestMessage.setCorrelationId(correlationId);

        processors.put(correlationId, processor);

        network.send(this.socketChannel, requestMessage.toJson().getBytes());
    }

    public void send(RequestMessage requestMessage, Processor processor) {
        checkArgument(requestMessage != null, "requestMessage cannot be null");
        checkArgument(requestMessage.getCorrelationId() != null, "processor.correlationId cannot be null");
        checkArgument(processor != null, "processor cannot be null");

        processors.put(requestMessage.getCorrelationId(), processor);

        network.send(this.socketChannel, requestMessage.toJson().getBytes());
    }

    void handle(NetworkEvent networkEvent) {
        logger.info("event received: " + networkEvent.getType());

        boolean processorUsed = false;

        if (networkEvent.getType() == NetworkEventType.CONNECT) {
            this.socketChannel = networkEvent.getSocketChannel();
        } else if (networkEvent.getType() == NetworkEventType.READ) {
            logger.info("decoding response");

            ResponseMessage responseMessage = ResponseMessage.fromBytes(networkEvent.getData());

            String correlationId = responseMessage.getCorrelationId();
            if (correlationId != null) {
                logger.info("getting processor for correlationId: " + correlationId);
                Processor processor = processors.remove(correlationId);
                if (processor != null) {
                    processor.process(responseMessage);
                    processorUsed = true;
                } else {
                    logger.error("processor not found");
                }
            } else {
                logger.info("notifying listeners of response");
                notifyChatListeners(responseMessage);
            }
        }

        if (!processorUsed) {
            events.get(networkEvent.getType()).add(networkEvent);
        }
    }

    void notifyChatListeners(ResponseMessage responseMessage) {
        for (ChatListener chatListener : chatListeners) {
            chatListener.onChatEvent(responseMessage);
        }
    }

    public void join(String user, String room) {
        logger.info("building request: type=JOIN, user=" + user + ", room=" + room);

        Map<String, String> payload = Maps.newHashMap();
        payload.put("user", user);
        payload.put("room", room);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(RequestMessageType.JOIN);
        requestMessage.setPayload(payload);

        logger.info("sending request");
        network.send(this.socketChannel, requestMessage.toJson().getBytes());

        waitForEvent(NetworkEventType.READ);
    }

    public List<ChatRoom> getChatRooms() {
        String correlationId = UUID.randomUUID().toString();
        logger.info("sending request: type=GET_CHAT_ROOMS, correlationId=" + correlationId);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(GET_ROOMS);
        requestMessage.setCorrelationId(correlationId);

        BlockingQueue<ResponseMessage> blockingQueue = new LinkedBlockingDeque<>();
        processors.put(correlationId, responseMessage -> {
            blockingQueue.add(responseMessage);
        });

        try {
            network.send(this.socketChannel, requestMessage.toJson().getBytes());

            ResponseMessage responseMessage = blockingQueue.poll(timeout, TimeUnit.SECONDS);
            return responseMessage.getRooms()
                    .stream()
                    .map(ChatRoom::new)
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public void sendMessage(String room, String user, String message) {
        logger.info("building send-message request: room=" + room + ", user=" + user + ", message=" + message);

        Map<String, String> payload = Maps.newHashMap();
        payload.put("message", message);
        payload.put("user", user);
        payload.put("room", room);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(RequestMessageType.MESSAGE);
        requestMessage.setPayload(payload);

        logger.info("sending request");
        network.send(this.socketChannel, requestMessage.toJson().getBytes());

        waitForEvent(NetworkEventType.READ);
    }

    public void leave(String room) {
        logger.info("building request: type=LEAVE, room=" + room);

        Map<String, String> payload = Maps.newHashMap();
        payload.put("room", room);

        RequestMessage requestMessage = new RequestMessage();
        requestMessage.setRequestMessageType(RequestMessageType.LEAVE);
        requestMessage.setPayload(payload);

        logger.info("sending request");
        network.send(this.socketChannel, requestMessage.toJson().getBytes());

        waitForEvent(NetworkEventType.READ);
    }

    private void waitForEvent(NetworkEventType networkEventType) {
        NetworkEvent event;
        try {
            logger.info("waiting for event=" + networkEventType);
            event = events.get(networkEventType).poll(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            event = null;
        }

        if (event == null) {
            throw new RuntimeException("event not received");
        }
    }
}

class EventWaiter implements NetworkListener {
    private static final Logger logger = Logger.getLogger(EventWaiter.class);
    private static final int TIMEOUT = 5;
    private final Map<NetworkEventType, BlockingQueue<NetworkEvent>> networkEvents = Maps.newConcurrentMap();

    public EventWaiter() {
        for (NetworkEventType networkEventType : NetworkEventType.values()) {
            networkEvents.put(networkEventType, new LinkedBlockingDeque<>());
        }
    }

    @Override
    public void onEvent(NetworkEvent networkEvent) {
        networkEvents.get(networkEvent.getType()).add(networkEvent);
    }

    public void waitForEvent(NetworkEventType networkEventType) {
        NetworkEvent event;
        try {
            event = networkEvents.get(networkEventType).poll(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            event = null;
        }

        if (event == null) {
            throw new RuntimeException("event not received");
        }
    }

}

