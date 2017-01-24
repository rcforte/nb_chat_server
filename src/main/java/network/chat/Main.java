package network.chat;

import chat.common.Message;
import network.Network;
import network.StringDecoder;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.function.Function;

import static chat.common.Message.from;
import static java.util.stream.Collectors.toList;

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
    network.addNetworkListener(new ChatListener(network, chat, ChatTranslator.translator()));
    network.bind(9999);
  }
}
