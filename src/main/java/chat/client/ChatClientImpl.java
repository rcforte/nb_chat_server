package chat.client;

import chat.common.Message;
import com.google.common.collect.Maps;
import network.NetworkClient;
import network.NetworkListener;
import network.chat.ChatRoom;
import network.chat.Translator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import static chat.common.Message.message;
import static chat.common.MessageType.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class ChatClientImpl implements ChatClient {
  private static final Logger logger = Logger.getLogger(ChatClientImpl.class);
  private static final long timeout = 5;

  private final String host;
  private final int port;
  private final NetworkClient network;
  private final List<ChatListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<String, Processor> processors = Maps.newConcurrentMap();
  private final Translator<byte[], List<Message>> translator;

  public ChatClientImpl(String host, int port, Translator<byte[], List<Message>> translator) {
    this.host = host;
    this.port = port;
    this.network = new NetworkClient();
    this.network.onRead(bytes -> {
      List<Message> msgs = translator.from(bytes);
      if (msgs != null && !msgs.isEmpty()) {
        for (Message msg : msgs) {
          if (msg.corrId() != null) {
            notifyProcessor(msg);
          } else {
            notifyListeners(msg);
          }
        }
      }
    });
    this.translator = translator;
  }

  @Override
  public void addListener(ChatListener lstn) {
    listeners.add(lstn);
  }

  @Override
  public void removeListener(ChatListener lstn) {
    listeners.remove(lstn);
  }

  @Override
  public void addNetworkListener(NetworkListener lstn) {
    network.addListener(lstn);
  }

  @Override
  public void removeNetworkListener(NetworkListener lstn) {
    network.removeListener(lstn);
  }

  @Override
  public void connect() throws IOException {
    network.connect(host, port);
  }

  @Override
  public void join(String room, String user) {
    send(message(JOIN).with("room", room).with("user", user));
  }

  @Override
  public void sendMessage(String room, String user, String message) {
    send(message(MESSAGE).with("message", message).with("user", user).with("room", room));
  }

  @Override
  public void leave(String room) {
    send(message(LEAVE).with("room", room));
  }

  @Override
  public List<ChatRoom> getChatRooms() {
    String corrId = corrId();
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    processors.put(corrId, msg -> queue.add(msg));
    try {
      send(message(GET_ROOMS, corrId));
      return queue.poll(timeout, SECONDS).get("rooms")
          .stream()
          .map(ChatRoom::new)
          .collect(toList());
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public void stop() {
    network.stop();
  }

  public void getChatRooms(Processor processor) {
    String corrId = UUID.randomUUID().toString();
    processors.put(corrId, processor);
    send(message(GET_ROOMS, corrId));
  }

  private void send(Message msg) {
    network.send(translator.to(newArrayList(msg)));
  }

  public void getChatRoomUsers(String room, Processor processor) {
    String corrId = UUID.randomUUID().toString();
    processors.put(corrId, processor);
    send(new Message(GET_ROOM_USERS, corrId).with("room", room));
  }

  public void send(Message message, Processor processor) {
    checkArgument(message != null, "message cannot be null");
    checkArgument(message.getCorrelationId() != null, "processor.correlationId cannot be null");
    checkArgument(processor != null, "processor cannot be null");
    processors.put(message.getCorrelationId(), processor);
    send(message);
  }

  private void notifyProcessor(Message msg) {
    Processor processor = processors.remove(msg.corrId());
    if (processor == null) {
      throw new RuntimeException("processor not found for corrId: " + msg.corrId());
    }
    processor.process(msg);
  }

  private void notifyListeners(Message msg) {
    for (ChatListener lstn : listeners) {
      lstn.onChatEvent(msg);
    }
  }

  private String corrId() {
    return UUID.randomUUID().toString();
  }
}
