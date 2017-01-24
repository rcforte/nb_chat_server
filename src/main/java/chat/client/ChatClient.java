package chat.client;

import chat.common.Message;
import com.google.common.collect.Maps;
import network.*;
import network.chat.ChatRoom;
import network.chat.Translator;
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

import static chat.common.Message.message;
import static chat.common.MessageType.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static network.NetworkEventType.READ;

public class ChatClient {

  private static final Logger logger = Logger.getLogger(ChatClient.class);

  private final String host;
  private final int port;
  private final long timeout = 5;
  private final Network network;
  private final NetworkListener networkListener = networkEvent -> handle(networkEvent);
  private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
  private final Map<String, Processor> processors = Maps.newConcurrentMap();
  private final Map<NetworkEventType, BlockingQueue<NetworkEvent>> events = Maps.newHashMap();
  private final Translator<byte[], List<Message>> translator;
  private SocketChannel channel;

  public ChatClient(String host, int port, Translator<byte[], List<Message>> translator) {
    this.host = host;
    this.port = port;
    this.network = new Network();
    this.translator = translator;

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
    sendMessage(new Message(GET_ROOMS, corrId));
  }

  private void sendMessage(Message msg) {
    network.send(channel, translator.to(newArrayList(msg)));
  }

  public void getChatRoomUsers(String room, Processor processor) {
    logger.info("sending get rooms request");
    String corrId = UUID.randomUUID().toString();
    processors.put(corrId, processor);
    sendMessage(new Message(GET_ROOM_USERS, corrId).with("room", room));
  }

  public void send(Message message, Processor processor) {
    checkArgument(message != null, "message cannot be null");
    checkArgument(message.getCorrelationId() != null, "processor.correlationId cannot be null");
    checkArgument(processor != null, "processor cannot be null");

    processors.put(message.getCorrelationId(), processor);

    sendMessage(message);
  }

  void handle(NetworkEvent event) {
    boolean processorUsed = false;

    if (event.getType() == NetworkEventType.CONNECT) {
      this.channel = event.getSocketChannel();
    } else if (event.getType() == READ) {
      List<Message> msgs = translator.from(event.getData());
      if (msgs == null || msgs.isEmpty()) {
        logger.info("responses could not be decoded");
      } else {
        logger.info(msgs.size() + " response(s) decoded");
        for (Message msg : msgs) {
          if (msg.corrId() != null) {
            Processor proc = processors.remove(msg.corrId());
            if (proc == null) {
              throw new RuntimeException("processor not found for corrId: " + msg.corrId());
            }
            proc.process(msg);
            processorUsed = true;
          } else {
            logger.info("using listeners");
            notifyChatListeners(msg);
          }
        }
      }
    }

    if (!processorUsed) {
      events.get(event.getType()).add(event);
    }
  }

  void notifyChatListeners(Message msg) {
    for (ChatListener chatListener : chatListeners) {
      chatListener.onChatEvent(msg);
    }
  }

  public void join(String user, String room) {
    sendMessage(new Message(JOIN).with("user", user).with("room", room));
    waitForEvent(READ);
  }

  public List<ChatRoom> getChatRooms() {
    String corrId = UUID.randomUUID().toString();
    BlockingQueue<Message> queue = new LinkedBlockingDeque<>();
    processors.put(corrId, msg -> queue.add(msg));
    try {
      sendMessage(message(GET_ROOMS, corrId));
      Message msg = queue.poll(timeout, TimeUnit.SECONDS);
      return msg.get("rooms").stream().map(ChatRoom::new).collect(toList());
    } catch (InterruptedException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  public void sendMessage(String room, String user, String message) {
    sendMessage(new Message(MESSAGE).with("message", message).with("user", user).with("room", room));
    waitForEvent(READ);
  }

  public void leave(String room) {
    sendMessage(new Message(LEAVE).with("room",room));
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
