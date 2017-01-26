package chat;

import chat.server.Chat;
import chat.server.ChatRoom;
import chat.server.ChatService;
import network.NetworkServer;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static network.NetworkEventType.READ;
import static chat.server.ChatTranslator.translator;
import static org.junit.Assert.assertEquals;

// Make blocking a decorator
// Make good separation of concerns in the server, adding layer of translator
public class ChatTest {
  private static final Logger logger = Logger.getLogger(ChatTest.class);
  private static final String HOST = "localhost";
  private static final int PORT = 9998;

  @Test
  public void functionalTest() throws Exception {
    List<String> rafaelMsgs = newArrayList();
    List<String> joeMsgs = newArrayList();

    // create a chat
    List<ChatRoom> rooms = newArrayList(
        new ChatRoom("Java Programming"),
        new ChatRoom("C++ Programming"));
    Chat chat = new Chat().rooms(rooms.toArray(new ChatRoom[]{}));

    // create a chat server
    NetworkServer server = new NetworkServer();
    server.addListener(new ChatService(server, chat, translator()));
    server.bind(PORT);

    // create a chat client
    BlockClient rafael = BlockClient.client(HOST, PORT);
    rafael.addListener(msg -> rafaelMsgs.addAll(msg.get("message")));

    logger.info("Rafael is connecting");
    rafael.connect();
    logger.info("Rafael is connected");

    logger.info("Rafael is joining");
    rafael.join(rooms.get(0).name(), "Rafael");
    logger.info("Rafael joined");

    // create another client
    BlockClient joe = BlockClient.client(HOST, PORT);
    joe.addListener(msg -> joeMsgs.addAll(msg.get("message")));

    logger.info("Joe is connecting");
    joe.connect();
    logger.info("Joe connected");

    logger.info("Joe is joining");
    joe.join(rooms.get(0).name(), "Joe");
    rafael.consume(READ);
    logger.info("Joe joined");

    // exchange a few messages
    logger.info("Joe is sending a message");
    joe.sendMessage(rooms.get(0).name(), "Joe", "Hello");
    rafael.consume(READ);
    logger.info("Joe sent a message");

    logger.info("Rafael is sending a message");
    rafael.sendMessage(rooms.get(0).name(), "Rafael", "Hello back!");
    joe.consume(READ);
    logger.info("Rafael sent a message");

    // leaving the chat
    logger.info("Joe is leaving");
    joe.leave(rooms.get(0).name());
    rafael.consume(READ);
    logger.info("Joe left");

    logger.info("Rafael is leaving");
    rafael.leave(rooms.get(0).name());
    logger.info("Rafael left");

    assertEquals("Problem with Rafael's messages",
        newArrayList(
            "Rafael has joined the chat",
            "Joe has joined the chat",
            "Joe says: Hello",
            "Rafael says: Hello back!",
            "Joe left the room"),
        rafaelMsgs);
    assertEquals("Problem with Joe's messages",
        newArrayList(
            "Joe has joined the chat",
            "Joe says: Hello",
            "Rafael says: Hello back!"),
        joeMsgs);

    // stop the server
    logger.info("stopping server");
    server.stop();
  }
}

