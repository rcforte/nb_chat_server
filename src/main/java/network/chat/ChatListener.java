package network.chat;

import chat.common.Message;
import chat.common.MessageType;
import network.Network;
import network.NetworkEvent;
import network.NetworkListener;

import java.nio.channels.SocketChannel;
import java.util.List;

import static chat.common.Message.message;
import static chat.common.MessageType.LEAVE;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static network.NetworkEventType.DISCONNECT;
import static network.NetworkEventType.READ;

public class ChatListener implements NetworkListener {
  private final Network network;
  private final Chat chat;
  private final Translator<byte[], List<Message>> translator;

  public ChatListener(Network network, Chat chat, Translator translator) {
    this.network = network;
    this.chat = chat;
    this.translator = translator;
  }

  @Override
  public void onEvent(NetworkEvent event) {
    List<Message> msgs = null;
    if (event.getType() == DISCONNECT) {
      msgs = newArrayList(new Message(LEAVE));
    } else if (event.getType() == READ) {
      msgs = translator.from(event.getData());
    }
    if (msgs != null) {
      msgs.stream().forEach(message -> {
        Command command = getCommand(event.getSocketChannel(), message);
        command.execute(chat, message);
      });
    }
  }

  Command getCommand(SocketChannel socketChannel, Message message) {
    if (message.getType() == MessageType.GET_ROOMS) {
      return new GetRoomsCommand(this, socketChannel);
    } else if (message.getType() == MessageType.GET_ROOM_USERS) {
      return new GetUsersCommand(this, socketChannel);
    } else if (message.getType() == MessageType.JOIN) {
      return new JoinRoomCommand(this, socketChannel);
    } else if (message.getType() == MessageType.MESSAGE) {
      return new SendMessageCommand(this, socketChannel);
    } else if (message.getType() == LEAVE) {
      return new LeaveRoomCommand(this, socketChannel);
    }
    return null;
  }

  void send(ChatRoom room, Message msg) {
    chat.send(network, room, translator.to(newArrayList(msg)));
  }

  void send(SocketChannel channel, Message msg) {
    network.send(channel, translator.to(newArrayList(msg)));
  }
}

class GetUsersCommand implements Command {
  private final ChatListener server;
  private final SocketChannel socketChannel;

  public GetUsersCommand(ChatListener server, SocketChannel socketChannel) {
    this.server = server;
    this.socketChannel = socketChannel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    String room = msg.getFirst("room");
    List<String> users = chat.getUsers(room).stream().map(user -> user.name()).collect(toList());
    server.send(socketChannel, message(msg.getType(), msg.corrId()).with("users", users));
  }
}

class GetRoomsCommand implements Command {
  private final ChatListener server;
  private final SocketChannel channel;

  public GetRoomsCommand(ChatListener server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message req) {
    server.send(channel, message(req.type(), req.corrId())
        .with("rooms", chat.rooms().stream().map(ChatRoom::name).collect(toList())));
  }
}

class SendMessageCommand implements Command {
  private final ChatListener server;
  private final SocketChannel channel;

  public SendMessageCommand(ChatListener server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message req) {
    String user = req.getFirst("user");
    String message = req.getFirst("message");
    String roomName = req.getFirst("room");
    String msg = user + " says: " + message;
    server.send(chat.room(roomName), message(req.type(), req.corrId()).with("message", msg));
  }
}

class JoinRoomCommand implements Command {
  private final ChatListener server;
  private final SocketChannel channel;

  public JoinRoomCommand(ChatListener server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    String user = msg.getFirst("user");
    String room = msg.getFirst("room");
    chat.join(channel, user, room);
    String str = String.format("%s has joined the chat", user);
    server.send(chat.room(room), message(msg.type(), msg.corrId()).with("message", str));
  }
}

class LeaveRoomCommand implements Command {
  private final ChatListener server;
  private final SocketChannel channel;

  public LeaveRoomCommand(ChatListener server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    ChatUser user = chat.user(channel);
    ChatRoom room = user.leave();
    String str = user.name() + " left the room";
    server.send(room, message(msg.type(), msg.corrId()).with("message", str));
  }
}

interface Command {
  void execute(Chat chat, Message message);
}
