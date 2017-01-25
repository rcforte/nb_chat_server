package network.chat;

import network.Network;
import org.apache.log4j.Logger;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    logger.info("creating chat");
    Chat chat = new Chat().rooms(
        new ChatRoom("Java Programming"),
        new ChatRoom("C++ Programming"),
        new ChatRoom("Python Programming"));

    logger.info("creating chat server");

    Network network = new Network();
    network.addNetworkListener(new ChatService(network, chat, ChatTranslator.translator()));
    network.bind(9999);
  }
}
