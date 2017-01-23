package network.chat;

import network.chat.ChatServer;
import org.apache.log4j.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        logger.info("creating chat");
        Chat chat = new Chat();
        chat.addRoom(new ChatRoom("Java Programming"));
        chat.addRoom(new ChatRoom("C++ Programming"));
        chat.addRoom(new ChatRoom("Python Programming"));

        logger.info("creating chat server");
        ChatServer chatServer = new ChatServer(9999);
        chatServer.setChat(chat);
        chatServer.start();
    }
}
