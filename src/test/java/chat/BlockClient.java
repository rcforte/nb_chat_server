package chat;

import chat.client.ChatClient;
import chat.client.ChatClientImpl;
import chat.client.ChatListener;
import chat.server.ChatRoom;
import com.google.common.collect.Maps;
import network.NetworkEvent;
import network.NetworkEventType;
import network.NetworkListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.concurrent.TimeUnit.SECONDS;
import static network.NetworkEventType.CONNECT;
import static network.NetworkEventType.READ;
import static chat.server.ChatTranslator.translator;

/**
 * Created by Rafael on 1/24/2017.
 */
class BlockClient implements ChatClient {
  private static final Logger logger = Logger.getLogger(BlockClient.class);
  private static final int TIMEOUT = 5;

  public static BlockClient client(String host, int port) {
    return new BlockClient(new ChatClientImpl(host, port, translator()));
  }

  private final ChatClient client;
  private final Map<NetworkEventType, BlockingQueue<NetworkEvent>> events = Maps.newConcurrentMap();

  public BlockClient(ChatClient client) {
    this.client = client;
    this.client.addNetworkListener(evt -> {
      if (evt.type() == READ || evt.type() == CONNECT) {
        events.get(evt.type()).add(evt);
      }
    });
    for (NetworkEventType type : NetworkEventType.values()) {
      events.put(type, new LinkedBlockingDeque<>(1));
    }
  }

  @Override
  public void addListener(ChatListener lstn) {
    client.addListener(lstn);
  }

  @Override
  public void removeListener(ChatListener lstn) {
    client.removeListener(lstn);
  }

  @Override
  public void addNetworkListener(NetworkListener lstn) {
    client.addNetworkListener(lstn);
  }

  @Override
  public void removeNetworkListener(NetworkListener lstn) {
    client.removeNetworkListener(lstn);
  }

  @Override
  public void connect() throws IOException {
    client.connect();
    consume(CONNECT);
  }

  @Override
  public void join(String room, String user) {
    client.join(room, user);
    consume(READ);
  }

  @Override
  public void sendMessage(String room, String user, String msg) {
    client.sendMessage(room, user, msg);
    consume(READ);
  }

  @Override
  public void leave(String room) {
    client.leave(room);
  }

  @Override
  public List<ChatRoom> getChatRooms() {
    return client.getChatRooms();
  }

  void consume(NetworkEventType type) {
    NetworkEvent event;
    try {
      event = events.get(type).poll(TIMEOUT, SECONDS);
    } catch (InterruptedException e) {
      event = null;
    }
    if (event == null) {
      throw new RuntimeException("event not received");
    }
  }
}
