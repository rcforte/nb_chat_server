package network.chat;

import chat.client.ChatClient;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChatTest {

    private static final Logger logger = Logger.getLogger(ChatTest.class);
    private static final int TIMEOUT = 300;

    @Test
    public void functionalTest() throws Exception {

        int port = 9998;
        List<String> rafaelMessages = Lists.newArrayList();
        List<String> joeMessages = Lists.newArrayList();

        // create a chat
        logger.info("creating chat");
        Chat chat = new Chat();
        chat.addRoom(new ChatRoom("Java Programming"));
        chat.addRoom(new ChatRoom("C++ Programming"));

        // create a chat server
        logger.info("starting server");
        ChatServer chatServer = new ChatServer(port);
        chatServer.setChat(chat);
        chatServer.start();

        // create a chat client
        ChatClient rafael = new ChatClient("localhost", port);
        rafael.addChatListener(responseMessage -> {
            rafaelMessages.addAll(responseMessage.getPayload().get("message"));
        });

        logger.info("Rafael is connecting");
        rafael.connect();

        logger.info("Rafael is getting chat rooms");
        List<ChatRoom> rooms = rafael.getChatRooms();

        logger.info("Rafael is joining");
        rafael.join("Rafael", rooms.get(0).getName());

        // create another client
        ChatClient joe = new ChatClient("localhost", port);
        joe.addChatListener(responseMessage -> {
            joeMessages.addAll(responseMessage.getPayload().get("message"));
        });

        logger.info("Joe is connecting");
        joe.connect();

        logger.info("Joe is joining");
        joe.join("Joe", rooms.get(0).getName());

        // exchange a few messages
        logger.info("Joe is sending a message");
        joe.sendMessage(rooms.get(0).getName(), "Joe", "Hello");

        logger.info("Rafael is sending a message");
        rafael.sendMessage(rooms.get(0).getName(), "Rafael", "Hello back!");
        Thread.sleep(TIMEOUT);

        // leaving the chat
        logger.info("Joe is leaving");
        joe.leave(rooms.get(0).getName());
        Thread.sleep(TIMEOUT);

        logger.info("Rafael is leaving");
        rafael.leave(rooms.get(0).getName());
        Thread.sleep(TIMEOUT);

        assertEquals("Problem with Rafael's messages",
                Lists.newArrayList(
                        "Rafael has joined the chat", "Joe has joined the chat",
                        "Joe says: Hello", "Rafael says: Hello back!", "Joe left the room"),
                rafaelMessages);
        assertEquals("Problem with Joe's messages",
                Lists.newArrayList("Joe has joined the chat", "Joe says: Hello", "Rafael says: Hello back!"),
                joeMessages);

        // stop the server
        logger.info("stopping server");
        chatServer.stop();
    }
}