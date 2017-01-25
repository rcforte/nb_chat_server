package chat.client;

import network.NetworkListener;
import network.chat.ChatRoom;

import java.io.IOException;
import java.util.List;

/**
 * Created by Rafael on 1/24/2017.
 */
public interface ChatClient {
  void addListener(ChatListener lstn);
  void removeListener(ChatListener lstn);
  void addNetworkListener(NetworkListener lstn);
  void removeNetworkListener(NetworkListener lstn);
  void connect() throws IOException;
  void join(String room, String user);
  void sendMessage(String room, String user, String msg);
  void leave(String room);
  List<ChatRoom> getChatRooms();
}
