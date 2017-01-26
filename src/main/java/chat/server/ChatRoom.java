package chat.server;

import com.google.common.collect.Maps;
import network.Network;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatRoom {
  private final String name;
  private final Map<String, ChatUser> users = Maps.newHashMap();

  public ChatRoom(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  public void send(Network network, byte[] bytes) {
    users.values().stream().forEach(user -> user.send(network, bytes));
  }

  public void user(String user, SocketChannel channel) {
    users.put(user, new ChatUser(channel, user, this));
  }

  public List<ChatUser> users() {
    return users.values().stream().collect(Collectors.toList());
  }

  public ChatUser user(SocketChannel channel) {
    return users.values().stream().filter(user -> user.has(channel)).findFirst().orElse(null);
  }

  public ChatUser remove(ChatUser user) {
    return users.remove(user.name());
  }
}
