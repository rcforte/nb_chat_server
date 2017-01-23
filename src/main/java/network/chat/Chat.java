package network.chat;

import com.google.common.collect.Maps;
import network.NonBlockingNetwork;
import org.apache.log4j.Logger;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Rafael on 1/21/2017.
 */
public class Chat {
    private static final Logger logger = Logger.getLogger(Chat.class);
    private final Map<String, ChatRoom> rooms = Maps.newHashMap();

    public void addRoom(ChatRoom room) {
        rooms.put(room.getName(), room);
    }

    public void removeRoom(ChatRoom room) {
        rooms.remove(room.getName());
    }

    public List<ChatRoom> getRooms() {
        return rooms.values().stream().collect(Collectors.toList());
    }

    public void join(SocketChannel socketChannel, String user, String room) {
        ChatRoom chatRoom = rooms.get(room);
        logger.info("adding " + user + " to " + chatRoom);
        chatRoom.addUser(socketChannel, user);
    }

    public void send(NonBlockingNetwork network, String room, byte[] bytes) {
        ChatRoom chatRoom = rooms.get(room);
        logger.info("sending message to room: " + chatRoom);
        chatRoom.sendMessage(network, bytes);
    }

    public List<ChatUser> getUsers(String chatRoom) {
        logger.info("getting room: " + chatRoom);
        ChatRoom room = rooms.get(chatRoom);
        logger.info("chat room: " + room);
        return room.getUsers();
    }

    public ChatUser removeUser(SocketChannel socketChannel) {
        ChatUser chatUser = null;
        logger.info("removing user from chat");
        for (ChatRoom room : rooms.values()) {
            logger.info("checking room: " + room.getName());
            chatUser = room.findAndRemoveUser(socketChannel);
            if (chatUser != null) {
                break;
            }
        }
        return chatUser;
    }

}
