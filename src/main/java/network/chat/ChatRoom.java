package network.chat;

import com.google.common.collect.Maps;
import network.NonBlockingNetwork;
import org.apache.log4j.Logger;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatRoom {
    private final static Logger logger = Logger.getLogger(ChatRoom.class);
    private final String name;
    private final Map<String, ChatUser> users = Maps.newHashMap();

    public ChatRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void sendMessage(NonBlockingNetwork network, byte[] bytes) {
        for (ChatUser user : users.values()) {
            logger.info("sending message to " + user.getName());
            user.send(network, bytes);
        }
    }

    public void addUser(SocketChannel socketChannel, String user) {
        users.put(user, new ChatUser(socketChannel, user));
        logger.info(user + " added to room " + this);
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "name='" + name + '\'' +
                ", users=" + users +
                '}';
    }

    public List<ChatUser> getUsers() {
        return users.values().stream().collect(Collectors.toList());
    }

    public ChatUser findAndRemoveUser(SocketChannel socketChannel) {
        ChatUser user = null;
        for (ChatUser u : users.values()) {
            if (u.isChannel(socketChannel)) {
                user = u;
                break;
            }
        }
        if (user != null) {
            logger.info("removing user=" + user.getName() + " from room=" + name);
            users.remove(user.getName());
        }
        return user;
    }
}
