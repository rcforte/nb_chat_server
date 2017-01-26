package chat.server;

import chat.common.Message;

import java.nio.channels.SocketChannel;
import java.util.List;

import static chat.common.Message.message;
import static java.util.stream.Collectors.toList;

/**
 * Created by Rafael on 1/24/2017.
 */
class GetRoomsCommand implements Command {
  private final ChatService server;
  private final SocketChannel channel;

  public GetRoomsCommand(ChatService server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message req) {
    List<String> rooms = chat.rooms().stream().map(ChatRoom::name).collect(toList());
    server.send(channel, message(req.type(), req.corrId()).with("rooms", rooms));
  }
}
