package chat.client;

import chat.common.Request;
import chat.common.Response;
import com.google.common.collect.Maps;
import network.*;
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

import static chat.common.RequestType.*;
import static com.google.common.base.Preconditions.checkArgument;
import static network.NetworkEventType.READ;

public class ChatClient {

  private static final Logger logger = Logger.getLogger(ChatClient.class);

  private final String host;
  private final int port;
  private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
  private final Network network;
  private final Map<String, Processor> processors = Maps.newConcurrentMap();
  private final Map<NetworkEventType, BlockingQueue<NetworkEvent>> events = Maps.newHashMap();
  private final long timeout = 5;
  private final Encoder<String> encoder = new TokenEncoder("\n");
  private SocketChannel socketChannel;
  private final NetworkListener networkListener = networkEvent -> handle(networkEvent);

  public ChatClient(String host, int port) {
    this.host = host;
    this.port = port;
    this.network = new Network();
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
    logger.info("sending get rooms request");
    String corrId = UUID.randomUUID().toString();
    processors.put(corrId, processor);
    sendMessage(new Request(GET_ROOMS, corrId));
  }

  private void sendMessage(Request request) {
    network.send(this.socketChannel, encoder.encode(Request.toJson(request)));
  }

  public void getChatRoomUsers(String room, Processor processor) {
    logger.info("sending get rooms request");
    String corrId = UUID.randomUUID().toString();
    processors.put(corrId, processor);
    sendMessage(new Request(GET_ROOM_USERS, corrId).with("room", room));
  }

  public void send(Request request, Processor processor) {
    checkArgument(request != null, "request cannot be null");
    checkArgument(request.getCorrelationId() != null, "processor.correlationId cannot be null");
    checkArgument(processor != null, "processor cannot be null");

    processors.put(request.getCorrelationId(), processor);

    sendMessage(request);
  }

  void handle(NetworkEvent event) {
    boolean processorUsed = false;

    if (event.getType() == NetworkEventType.CONNECT) {
      this.socketChannel = event.getSocketChannel();
    } else if (event.getType() == READ) {
      List<String> responses = encoder.decode(event.getData());
      if (responses == null || responses.isEmpty()) {
        logger.info("responses could not be decoded");
      } else {
        logger.info(responses.size() + " response(s) decoded");
        for (String response : responses) {
          Response resp = Response.from(response);
          if (resp.corrId() != null) {
            logger.info("using processor");
            Processor processor = processors.remove(resp.corrId());
            if (processor == null) {
              throw new RuntimeException("processor not found for corrId: " + resp.corrId());
            }
            processor.process(resp);
            processorUsed = true;
          } else {
            logger.info("using listeners");
            notifyChatListeners(resp);
          }
        }
      }
    }

    if (!processorUsed) {
      events.get(event.getType()).add(event);
    }
  }

  void notifyChatListeners(Response response) {
    for (ChatListener chatListener : chatListeners) {
      chatListener.onChatEvent(response);
    }
  }

  public void join(String user, String room) {
    sendMessage(new Request(JOIN).with("user", user).with("room", room));
    waitForEvent(READ);
  }

  public List<ChatRoom> getChatRooms() {
    String corrId = UUID.randomUUID().toString();
    Request request = new Request(GET_ROOMS,corrId);
    BlockingQueue<Response> queue = new LinkedBlockingDeque<>();
    processors.put(corrId, response -> queue.add(response));
    try {
      sendMessage(request);
      Response response = queue.poll(timeout, TimeUnit.SECONDS);
      return response.get("rooms").stream().map(ChatRoom::new).collect(Collectors.toList());
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public void sendMessage(String room, String user, String message) {
    sendMessage(new Request(MESSAGE).with("message", message).with("user", user).with("room", room));
    waitForEvent(READ);
  }

  public void leave(String room) {
    sendMessage(new Request(LEAVE).with("room",room));
    waitForEvent(READ);
  }

  private void waitForEvent(NetworkEventType type) {
    NetworkEvent event;
    try {
      event = events.get(type).poll(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      event = null;
    }
    if (event == null) {
      throw new RuntimeException("event not received");
    }
  }
}
