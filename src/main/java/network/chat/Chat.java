package network.chat;

import com.google.common.collect.Maps;
import network.Network;
import org.apache.log4j.Logger;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Rafael on 1/21/2017.
 */
public class Chat {
  private final Map<String, ChatRoom> rooms = Maps.newHashMap();

  public void room(ChatRoom room) {
    rooms.put(room.name(), room);
  }

  public ChatRoom room(String roomName) {
    return rooms.get(roomName);
  }

  public List<ChatRoom> rooms() {
    return rooms.values().stream().collect(Collectors.toList());
  }

  public void join(SocketChannel channel, String user, String room) {
    rooms.get(room).user(user, channel);
  }

  public void send(Network network, ChatRoom room, byte[] bytes) {
    room.send(network, bytes);
  }

  public List<ChatUser> getUsers(String chatRoom) {
    return rooms.get(chatRoom).users();
  }

  public ChatUser user(SocketChannel channel) {
    return rooms.values().stream()
        .map(room -> room.user(channel))
        .filter(user -> user != null)
        .findFirst()
        .orElse(null);
  }
}
