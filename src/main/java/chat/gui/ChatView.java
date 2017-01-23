package chat.gui;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Created by Rafael on 1/15/2017.
 */
class ChatView extends JPanel {
    private static final Logger logger = Logger.getLogger(ChatView.class);

    private final ChatController chatController;
    private final ChatModel chatModel;
    private final ChatModelListener chatModelListener = chatModel -> handleModelUpdate(chatModel);

    private JList chatRoomList;
    private JList chatRoomUserList;
    private JButton joinButton;
    private JTextField userField;
    private JPanel northPanel;
    private JPanel centerPanel;
    private JTextArea messagesArea;
    private JTextField messageField;
    private JButton sendMessageButton;


    public ChatView(ChatController chatController, ChatModel chatModel) {
        this.chatController = chatController;
        this.chatModel = chatModel;
        this.chatModel.addChatModelListener(chatModelListener);
        buildLayout();
        this.chatController.getChatRooms();

    }

    void buildLayout() {
        messagesArea = new JTextArea();
        messageField = new JTextField(15);
        sendMessageButton = new JButton("Send message");
        sendMessageButton.addActionListener(e -> {
            String message = messageField.getText();
            chatController.sendMessage(chatModel.getRoom(), chatModel.getUser(), message);
        });

        userField = new JTextField(15);
        joinButton = new JButton("Join");
        joinButton.addActionListener(e -> {
            chatController.join(userField.getText(), (String) chatRoomList.getSelectedValue());

            northPanel.removeAll();
            northPanel.add(new JLabel("Message"));
            northPanel.add(messageField);
            northPanel.add(sendMessageButton);
            northPanel.updateUI();

            centerPanel.removeAll();
            JScrollPane sp = new JScrollPane(messagesArea);
            sp.setBorder(new TitledBorder(chatModel.getRoom()));
            centerPanel.add(sp);
            centerPanel.updateUI();
        });
        northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout());
        northPanel.add(new JLabel("Join as"));
        northPanel.add(userField);
        northPanel.add(joinButton);

        chatRoomList = new JList(new SimpleListModel());
        chatRoomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatRoomList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int selectedIndex = chatRoomList.getSelectedIndex();
            SimpleListModel simpleListModel = (SimpleListModel) chatRoomList.getModel();
            String chatRoom = (String) simpleListModel.getElementAt(selectedIndex);
            chatController.getChatRoomUsers(chatRoom);
        });
        chatRoomUserList = new JList(new SimpleListModel());
        chatRoomUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(1, 2));
        JScrollPane sp1 = new JScrollPane(chatRoomList);
        sp1.setBorder(new TitledBorder("Chat Rooms"));
        centerPanel.add(sp1);
        JScrollPane sp2 = new JScrollPane(chatRoomUserList);
        sp2.setBorder(new TitledBorder("Users"));
        centerPanel.add(sp2);

        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, northPanel);
        add(BorderLayout.CENTER, centerPanel);
    }

    void handleModelUpdate(ChatModel chatModel) {
        logger.info("updating rooms view");
        SimpleListModel chatRoomsListModel = (SimpleListModel) chatRoomList.getModel();
        chatRoomsListModel.setValues(chatModel.getRooms());
        chatRoomList.updateUI();

        logger.info("updating users view");
        SimpleListModel chatRoomUsersModel = (SimpleListModel) chatRoomUserList.getModel();
        java.util.List<String> chatRoomUsers = chatModel.getUsers();
        if (chatRoomUsers != null) {
            chatRoomUsersModel.setValues(chatRoomUsers);
            chatRoomUserList.updateUI();
        }

        logger.info("updating messages view");
        messagesArea.setText("");
        for (String message : chatModel.getMessages()) {
            messagesArea.append(message+"\n");
        }
    }
}
