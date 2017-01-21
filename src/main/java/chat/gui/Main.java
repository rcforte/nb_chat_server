package chat.gui;

import chat.client.ChatClient;
import org.apache.log4j.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ChatClient chatClient = new ChatClient("localhost", 9999);
                chatClient.connect();

                ChatModel chatModel = new ChatModel();
                ChatController chatController = new ChatController(chatModel, chatClient);
                ChatView chatView = new ChatView(chatController, chatModel);

                JFrame frame = new ChatFrame(chatModel, chatView, chatController);
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
