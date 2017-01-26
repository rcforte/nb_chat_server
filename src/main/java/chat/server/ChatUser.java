package chat.server;

import network.Network;

import java.nio.channels.SocketChannel;

public class ChatUser {
  private final SocketChannel channel;
  private final String name;
  private final ChatRoom room;

  public ChatUser(SocketChannel channel, String name, ChatRoom room) {
    this.channel = channel;
    this.name = name;
    this.room = room;
  }

  public String name() {
    return name;
  }

  public void send(Network network, byte[] bytes) {
    network.send(channel, bytes);
  }

  public boolean has(SocketChannel channel) {
    return this.channel == channel;
  }

  public ChatRoom leave() {
    room.remove(this);
    return room;
  }
}
