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
    this.chatClient.addChatListener(responseMessage -> {
      List<String> messages = responseMessage.get("message");
      logger.info("received response: " + messages);
      for (String message : messages) {
        chatModel.addMessage(message);
      }
    });
  }

  public void getChatRooms() {
    logger.info("getting chat rooms");
    executorService.execute(() -> {
      chatClient.getChatRooms(responseMessage -> {
        SwingUtilities.invokeLater(() -> {
          List<String> rooms = responseMessage.get("rooms");
          logger.info("received rooms: " + rooms);
          chatModel.setRooms(rooms);
        });
      });
    });
  }

  public void getChatRoomUsers(String chatRoom) {
    executorService.execute(() -> {
      chatClient.getChatRoomUsers(chatRoom, responseMessage -> {
        SwingUtilities.invokeLater(() -> {
          List<String> chatUsers = responseMessage.get("chatUsers");
          if (chatUsers != null) {
            chatModel.setUsers(chatUsers);
          }
        });
      });
    });
  }

  public void join(String user, String room) {
    executorService.execute(() -> {
      logger.info("join: user=" + user + ", room=" + room);
      chatModel.setUser(user);
      chatModel.setRoom(room);
      chatClient.join(user, room);
    });
  }

  public void sendMessage(String room, String user, String message) {
    executorService.execute(() -> {
      logger.info("send: room=" + room + ", user=" + user + ", message=" + message);
      chatClient.sendMessage(room, user, message);
    });
  }
}
