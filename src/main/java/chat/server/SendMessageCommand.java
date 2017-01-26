package chat.server;

import chat.common.Message;

import java.nio.channels.SocketChannel;

import static chat.common.Message.message;

/**
 * Created by Rafael on 1/24/2017.
 */
class SendMessageCommand implements Command {
  private final ChatService server;
  private final SocketChannel channel;

  public SendMessageCommand(ChatService server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    String user = msg.getFirst("user");
    String message = msg.getFirst("message");
    String roomName = msg.getFirst("room");
    String str = user + " says: " + message;
    server.send(chat.room(roomName), message(msg.type(), msg.corrId()).with("message", str));
  }
}
