package chat.server;

import chat.common.Message;

import java.nio.channels.SocketChannel;
import java.util.List;

import static chat.common.Message.message;
import static java.util.stream.Collectors.toList;

/**
 * Created by Rafael on 1/24/2017.
 */
class GetUsersCommand implements Command {
  private final ChatService server;
  private final SocketChannel socketChannel;

  public GetUsersCommand(ChatService server, SocketChannel socketChannel) {
    this.server = server;
    this.socketChannel = socketChannel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    String room = msg.get("room").get(0);
    List<String> users = chat.getUsers(room).stream().map(user -> user.name()).collect(toList());
    server.send(socketChannel, message(msg.getType(), msg.corrId()).with("users", users));
  }
}
