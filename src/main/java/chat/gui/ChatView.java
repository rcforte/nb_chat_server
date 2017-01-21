package chat.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Rafael on 1/15/2017.
 */
class ChatView extends JPanel {
    private final ChatController chatController;
    private final ChatModel chatModel;
    private final ChatModelListener chatModelListener = chatModel -> handleModelUpdate(chatModel);

    private JList chatRoomList;
    private JList chatRoomUserList;


    public ChatView(ChatController chatController, ChatModel chatModel) {
        this.chatController = chatController;
        this.chatModel = chatModel;
        this.chatModel.addChatModelListener(chatModelListener);
        buildLayout();
        this.chatController.getChatRooms();
    }

    void buildLayout() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new GridLayout(1, 2));
        northPanel.add(new JLabel("Name"));
        northPanel.add(new JTextField());

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

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(1, 2));
        centerPanel.add(chatRoomList);
        centerPanel.add(chatRoomUserList);

        setLayout(new BorderLayout());
        add(BorderLayout.NORTH, northPanel);
        add(BorderLayout.CENTER, centerPanel);
    }

    void handleModelUpdate(ChatModel chatModel) {
        SimpleListModel chatRoomsListModel = (SimpleListModel) chatRoomList.getModel();
        chatRoomsListModel.setValues(chatModel.getChatRooms());
        chatRoomList.updateUI();

        SimpleListModel chatRoomUsersModel = (SimpleListModel) chatRoomUserList.getModel();
        java.util.List<String> chatRoomUsers = chatModel.getChatRoomUsers();
        if (chatRoomUsers != null) {
            chatRoomUsersModel.setValues(chatRoomUsers);
            chatRoomUserList.updateUI();
        }
    }
}
