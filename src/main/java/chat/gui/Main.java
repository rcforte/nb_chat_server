package chat.gui;

import chat.client.ChatClient;
import network.chat.ChatTranslator;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.IOException;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class);

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        ChatClient client = new ChatClient("localhost", 9999, ChatTranslator.translator());
        client.connect();

        ChatModel model = new ChatModel();
        ChatController controller = new ChatController(model, client);
        ChatView view = new ChatView(controller, model);

        JFrame frame = new ChatFrame(model, view, controller);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Chat Application");
        frame.setSize(400, 200);
        frame.setVisible(true);
      } catch (IOException e) {
        logger.error("Cannot connect to server", e);
      }
    });
  }
}
