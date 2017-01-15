package chat.gui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import chat.client.ChatClient;
import chat.client.ChatListener;
import chat.common.ResponseMessage;
import org.apache.log4j.Logger;


public class ChatFrame extends JFrame {
	private static final Logger logger = Logger.getLogger(ChatFrame.class);

	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private final ChatListener chatListener = new ChatListener() {

		@Override
		public void onChatEvent(ResponseMessage responseMessage) {

		}
	};

	private ChatClient chatClient;

	public ChatFrame() {
		super("Chat Window");
		executorService.execute(() -> startChatClient());
	}

	private void startChatClient() {
		try {
			chatClient = new ChatClient("localhost", 9999);
			chatClient.addChatListener(chatListener);
			chatClient.connect();
		} catch (IOException e) {
			logger.error("Cannot connect to server", e);
		}
	}

	private void buildChatRoomsView(List<String> rooms) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		ButtonGroup group = new ButtonGroup();
		for(String room : rooms) {
			JRadioButton button = new JRadioButton(room);
			group.add(button);
			panel.add(button);
		}
		
		JScrollPane scrollPane = new JScrollPane(panel);
		scrollPane.setBorder(new TitledBorder("Chat Rooms"));
		add(BorderLayout.CENTER, scrollPane);
		
		revalidate();
	}
}
