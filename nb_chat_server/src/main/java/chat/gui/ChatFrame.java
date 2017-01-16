package chat.gui;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

// TODO: code view
// TODO: streamline api in controller
// TODO: get objects intead of messy maps of lists
// TODO: dependency injection
// TODO: write more tests with multiple threads
// TODO: message framing
public class ChatFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(ChatFrame.class);

    private final ChatModel chatModel;
    private final ChatView chatView;
    private final ChatController chatController;

    public ChatFrame(ChatModel chatModel, ChatView chatView, ChatController chatController) {
        this.chatModel = chatModel;
        this.chatView = chatView;
        this.chatController = chatController;

        getContentPane().add(BorderLayout.CENTER, chatView);
    }
}

