package chat.server;

import chat.common.Message;
import network.Network;
import network.NetworkEvent;
import network.NetworkListener;
import network.Translator;

import java.nio.channels.SocketChannel;
import java.util.List;

import static chat.common.MessageType.*;
import static com.google.common.collect.Lists.newArrayList;
import static network.NetworkEventType.DISCONNECT;
import static network.NetworkEventType.READ;

public class ChatService implements NetworkListener {
  private final Network network;
  private final Chat chat;
  private final Translator<byte[], List<Message>> translator;

  public ChatService(Network network, Chat chat, Translator translator) {
    this.network = network;
    this.chat = chat;
    this.translator = translator;
  }

  @Override
  public void onEvent(NetworkEvent evt) {
    List<Message> msgs = null;
    if (evt.type() == DISCONNECT) {
      msgs = newArrayList(new Message(LEAVE));
    } else if (evt.type() == READ) {
      msgs = translator.from(evt.getData());
    }

    if (msgs != null) {
      msgs.stream().forEach(msg -> cmd(evt.channel(), msg).execute(chat, msg));
    }
  }

  Command cmd(SocketChannel channel, Message msg) {
    if (msg.type() == GET_ROOMS) {
      return new GetRoomsCommand(this, channel);
    } else if (msg.type() == GET_ROOM_USERS) {
      return new GetUsersCommand(this, channel);
    } else if (msg.type() == JOIN) {
      return new JoinRoomCommand(this, channel);
    } else if (msg.type() == MESSAGE) {
      return new SendMessageCommand(this, channel);
    } else if (msg.type() == LEAVE) {
      return new LeaveRoomCommand(this, channel);
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

