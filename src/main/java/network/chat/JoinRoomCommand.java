package network.chat;

import chat.common.Message;
import org.apache.log4j.Logger;

import java.nio.channels.SocketChannel;

import static chat.common.Message.message;

/**
 * Created by Rafael on 1/24/2017.
 */
class JoinRoomCommand implements Command {
  private static final Logger logger = Logger.getLogger(JoinRoomCommand.class);
  private final ChatService server;
  private final SocketChannel channel;

  public JoinRoomCommand(ChatService server, SocketChannel channel) {
    this.server = server;
    this.channel = channel;
  }

  @Override
  public void execute(Chat chat, Message msg) {
    String room = msg.get("room").get(0);
    String user = msg.get("user").get(0);
    chat.join(channel, room, user);
    String str = String.format("%s has joined the chat", user);
    server.send(chat.room(room), message(msg.type(), msg.corrId()).with("message", str));
  }
}
