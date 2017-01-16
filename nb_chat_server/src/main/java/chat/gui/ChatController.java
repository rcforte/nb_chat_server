package chat.gui;

import chat.client.ChatClient;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Rafael on 1/15/2017.
 */
class ChatController {
    private static final Logger logger = Logger.getLogger(ChatController.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public ChatController(ChatModel chatModel, ChatClient chatClient) {
        this.chatModel = chatModel;
        this.chatClient = chatClient;
    }

    public void getChatRooms() {
        executorService.execute(() -> {
            chatClient.getChatRooms(responseMessage -> {
                SwingUtilities.invokeLater(() -> {
                    List<String> rooms = responseMessage.getRooms();
                    chatModel.setChatRooms(rooms);
                });
            });
        });
    }

    public void getChatRoomUsers(String chatRoom) {
        executorService.execute(() -> {
            chatClient.getChatRoomUsers(chatRoom, responseMessage -> {
                SwingUtilities.invokeLater(() -> {
                    Map<String, List<String>> payload = responseMessage.getPayload();
                    System.out.println(payload);
                    List<String> chatUsers = payload.get("chatUsers");
                    if (chatUsers != null) {
                        chatModel.setChatRoomUsers(chatUsers);
                    }
                });
            });
        });
    }
}
