package chat.gui;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by Rafael on 1/15/2017.
 */
class ChatModel {
    private final List<ChatModelListener> chatModelListeners = Lists.newCopyOnWriteArrayList();
    private String name;
    private List<String> chatRooms;
    private List<String> chatRoomUsers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getChatRooms() {
        return chatRooms;
    }

    public void setChatRooms(List<String> chatRooms) {
        this.chatRooms = chatRooms;
        notifyChatModelListeners();
    }

    public List<String> getChatRoomUsers() {
        return chatRoomUsers;
    }

    public void setChatRoomUsers(List<String> chatRoomUsers) {
        this.chatRoomUsers = chatRoomUsers;
        notifyChatModelListeners();
    }

    public void addChatModelListener(ChatModelListener chatModelListener) {
        chatModelListeners.add(chatModelListener);
    }

    public void removeChatModelListener(ChatModelListener chatModelListener) {
        chatModelListeners.remove(chatModelListener);
    }

    public void notifyChatModelListeners() {
        for (ChatModelListener chatModelListener : chatModelListeners) {
            chatModelListener.onModelUpdate(this);
        }
    }
}
