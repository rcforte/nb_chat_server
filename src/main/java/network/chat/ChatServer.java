package network.chat;

import chat.common.Request;
import chat.common.RequestType;
import chat.common.Response;
import network.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.stream.Collectors;

import static chat.common.RequestType.LEAVE;
import static chat.common.Response.response;
import static com.google.common.collect.Lists.newArrayList;
import static network.NetworkEventType.DISCONNECT;
import static network.NetworkEventType.READ;

public class ChatServer {
  private static final Logger logger = Logger.getLogger(ChatServer.class);

  private final int port;
  private final Network network;
  private final NetworkListener networkListener = networkEvent -> handle(networkEvent);
  private final Encoder<String> encoder = new TokenEncoder("\n");

  private Chat chat;

  public ChatServer(int port) {
    this.port = port;
    this.network = new Network();
    this.network.addNetworkListener(networkListener);
  }

  public void setChat(Chat chat) {
    this.chat = chat;
  }

  public void start() throws IOException {
    logger.info("starting server");
    network.bind(port);
  }

  public void stop() throws IOException {
    logger.info("stopping server...");
    network.stop();
  }

  void handle(NetworkEvent event) {
    Translator translator = new Translator(encoder);
    List<Request> requests = translator.translate(event);
    if (requests != null) {
      for (Request request : requests) {
        Command command = getCommand(event.getSocketChannel(), request);
        command.execute(chat, request);
      }
    }
  }

  Command getCommand(SocketChannel socketChannel, Request request) {
    if (request.getType() == RequestType.GET_ROOMS) {
      return new GetRoomsCommand(this, socketChannel);
    } else if (request.getType() == RequestType.GET_ROOM_USERS) {
      return new GetUsersCommand(this, socketChannel);
    } else if (request.getType() == RequestType.JOIN) {
      return new JoinRoomCommand(this, socketChannel);
    } else if (request.getType() == RequestType.MESSAGE) {
      return new SendMessageCommand(this, socketChannel);
    } else if (request.getType() == LEAVE) {
      return new LeaveRoomCommand(this, socketChannel);
    }
    return null;
  }

  void send(ChatRoom room, Response resp) {
    chat.send(network, room, encoder.encode(resp.json()));
  }

  void send(SocketChannel socketChannel, Response response) {
    network.send(socketChannel, encoder.encode(response.json()));
  }
}

class Translator {
  private final Encoder<String> encoder;

  public Translator(Encoder<String> encoder) {
    this.encoder = encoder;
  }

  public List<Request> translate(NetworkEvent event) {
    List<Request> requests = null;
    if (event.getType() == READ) {
      requests = newArrayList();
      for (String decoded : encoder.decode(event.getData())) {
        requests.add(Request.fromString(decoded));
      }
    } else if (event.getType() == DISCONNECT) {
      requests = newArrayList(new Request(LEAVE));
    }
    return requests;
  }
}

class GetUsersCommand implements Command {
  private static final Logger logger = Logger.getLogger(GetUsersCommand.class);
  private final ChatServer server;
  private final SocketChannel socketChannel;

  public GetUsersCommand(ChatServer server, SocketChannel socketChannel) {
    this.server = server;
    this.socketChannel = socketChannel;
  }

  @Override
  public void execute(Chat chat, Request request) {
    String room = request.get("room");
    List<String> users = chat.getUsers(room).stream().map(user -> user.name()).collect(Collectors.toList());
    server.send(socketChannel, new Response(request.getCorrelationId()).with("users", users));
  }
}

class GetRoomsCommand implements Command {
  private final ChatServer server;
  private final SocketChannel channel;

  public GetRoomsCommand(ChatServer server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Request req) {
    List<String> rooms = chat.rooms().stream().map(room -> room.name()).collect(Collectors.toList());
    server.send(channel, response(req.corrId()).with("rooms", rooms));
  }
}

class SendMessageCommand implements Command {
  private final ChatServer server;
  private final SocketChannel channel;

  public SendMessageCommand(ChatServer server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Request req) {
    String user = req.get("user");
    String message = req.get("message");
    String roomName = req.get("room");
    String msg = user + " says: " + message;
    server.send(chat.room(roomName), new Response().with("message", msg));
  }
}

class JoinRoomCommand implements Command {
  private final ChatServer server;
  private final SocketChannel channel;

  public JoinRoomCommand(ChatServer server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Request request) {
    String user = request.get("user");
    String room = request.get("room");
    chat.join(channel, user, room);
    String msg = String.format("%s has joined the chat", user);
    server.send(chat.room(room), response().with("message", msg));
  }
}

class LeaveRoomCommand implements Command {
  private final ChatServer server;
  private final SocketChannel channel;

  public LeaveRoomCommand(ChatServer server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Request request) {
    ChatUser user = chat.user(channel);
    ChatRoom room = user.leave();
    String msg = user.name() + " left the room";
    server.send(room, response().with("message", msg));
  }
}

interface Command {
  void execute(Chat chat, Request request);
}
