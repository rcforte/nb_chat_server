package network.chat;

import chat.client.ChatClient;
import network.Network;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;

// Make blocking a decorator
// Make good separation of concerns in the server, adding layer of translator
public class ChatTest {

  private static final Logger logger = Logger.getLogger(ChatTest.class);
  private static final int TIMEOUT = 1000;

  @Test
  public void functionalTest() throws Exception {

    int port = 9998;
    List<String> rafaelMessages = newArrayList();
    List<String> joeMessages = newArrayList();

    // create a chat
    Chat chat = new Chat().rooms(
        new ChatRoom("Java Programming"),
        new ChatRoom("C++ Programming"));

    // create a chat server
    Network network = new Network();
    network.addNetworkListener(new ChatListener(network, chat, ChatTranslator.translator()));
    network.bind(port);

    // create a chat client
    ChatClient rafael = new ChatClient("localhost", port, ChatTranslator.translator());
    rafael.addChatListener(responseMessage -> {
      rafaelMessages.addAll(responseMessage.get("message"));
    });

    logger.info("Rafael is connecting");
    rafael.connect();

    logger.info("Rafael is getting chat rooms");
    List<ChatRoom> rooms = rafael.getChatRooms();

    logger.info("Rafael is joining");
    rafael.join("Rafael", rooms.get(0).name());

    // create another client
    ChatClient joe = new ChatClient("localhost", port, ChatTranslator.translator());
    joe.addChatListener(responseMessage -> {
      joeMessages.addAll(responseMessage.get("message"));
    });

    logger.info("Joe is connecting");
    joe.connect();

    logger.info("Joe is joining");
    joe.join("Joe", rooms.get(0).name());

    // exchange a few messages
    logger.info("Joe is sending a message");
    joe.sendMessage(rooms.get(0).name(), "Joe", "Hello");

    logger.info("Rafael is sending a message");
    rafael.sendMessage(rooms.get(0).name(), "Rafael", "Hello back!");
    sleep(TIMEOUT);

    // leaving the chat
    logger.info("Joe is leaving");
    joe.leave(rooms.get(0).name());
    sleep(TIMEOUT);

    logger.info("Rafael is leaving");
    rafael.leave(rooms.get(0).name());
    sleep(TIMEOUT);

    assertEquals("Problem with Rafael's messages",
        newArrayList(
            "Rafael has joined the chat", "Joe has joined the chat",
            "Joe says: Hello", "Rafael says: Hello back!", "Joe left the room"),
        rafaelMessages);
    assertEquals("Problem with Joe's messages",
        newArrayList("Joe has joined the chat", "Joe says: Hello", "Rafael says: Hello back!"),
        joeMessages);

    // stop the server
    logger.info("stopping server");
    network.stop();
  }
}