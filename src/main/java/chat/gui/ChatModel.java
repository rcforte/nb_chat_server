package chat.gui;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by Rafael on 1/15/2017.
 */
class ChatModel {
    private final List<ChatModelListener> modelListeners = Lists.newCopyOnWriteArrayList();
    private String name;
    private List<String> rooms;
    private List<String> users;
    private List<String> messages = Lists.newArrayList();
    private String user;
    private String room;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRooms() {
        return rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
        notifyChatModelListeners();
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
        notifyChatModelListeners();
    }

    public void addChatModelListener(ChatModelListener chatModelListener) {
        modelListeners.add(chatModelListener);
    }

    public void removeChatModelListener(ChatModelListener chatModelListener) {
        modelListeners.remove(chatModelListener);
    }

    public void notifyChatModelListeners() {
        for (ChatModelListener chatModelListener : modelListeners) {
            chatModelListener.onModelUpdate(this);
        }
    }

    public void addMessage(String message) {
        messages.add(message);
        notifyChatModelListeners();
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
