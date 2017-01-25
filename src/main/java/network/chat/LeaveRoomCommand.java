package network.chat;

import chat.common.Message;

import java.nio.channels.SocketChannel;

import static chat.common.Message.message;

/**
 * Created by Rafael on 1/24/2017.
 */
class LeaveRoomCommand implements Command {
  private final ChatService server;
  private final SocketChannel channel;

  public LeaveRoomCommand(ChatService server, SocketChannel channel) {
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
