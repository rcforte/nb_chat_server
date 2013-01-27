package nbchat.gui;

import java.awt.BorderLayout;
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

import nbchat.client.ChatClient;
import nbchat.client.ChatListener;

public class ChatFrame extends JFrame {
	private ChatClient m_chatClient;
	private final ExecutorService m_executor = Executors.newCachedThreadPool();

	public ChatFrame() {
		super("Chat Window");

		// Client blocks waiting for backend data, so put in another thread
		m_executor.execute(new Runnable() {
			@Override
			public void run() {
				startChatClient();
			}
		});
	}

	private void startChatClient() {
		m_chatClient = new ChatClient();
		m_chatClient.addListener(new ChatListener() {
			@Override
			public void onConnected() {
				m_chatClient.getChatRooms();
			}

			@Override
			public void onChatRooms(final List<String> rooms) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						buildChatRoomsView(rooms);
					}
				});
			}

			@Override
			public void onFail(String reason) {
				System.out.println(reason);
			}
		});
		m_chatClient.connect();
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
