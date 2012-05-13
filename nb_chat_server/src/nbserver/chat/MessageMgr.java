package nbserver.chat;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MessageMgr {
	private static final long CLEANUP_INTERVAL = 50000L;
	
	private final Map<String, List<Message>> m_messages = Collections.synchronizedMap(new LinkedHashMap<String, List<Message>>());
	private final Timer m_timer = new Timer();
	private final TimerTask m_cleanupTask = new TimerTask() {
		@Override public void run() {
			handleCleanup();
		}
	};
	
	public MessageMgr() {
		m_timer.schedule(m_cleanupTask, 0L, CLEANUP_INTERVAL);
	}
	
	private synchronized void handleCleanup() {
		System.out.println("cleanup running...");

		// Go through all messages and remove old ones.
		List<String> toBeRemoved = new ArrayList<String>();
		for (Map.Entry<String, List<Message>> entry : m_messages.entrySet()) {

			// Remove the messages.
			List<Message> messages = entry.getValue();
			for (Iterator<Message> it = messages.iterator(); it.hasNext(); ) {
				Message message = it.next();
				if (message.isOld()) {
					it.remove();
				}
			}
			
			// Mark empty message map entries for removal.
			if (messages.isEmpty()) {
				toBeRemoved.add(entry.getKey());
			}
		}
		
		// Remove marked map entries.
		if (!toBeRemoved.isEmpty()) {
			for (String key : toBeRemoved) {
				m_messages.remove(key);
			}
		}
	}
	
	public void addMessage(SelectionKey selKey, String msg) {
		// Get the IP address identifying the selection key.
		String id = getKeyIdentifier(selKey);
		
		// Get the list of pending messages. Create a new list if needed.
		List<Message> pendingMessages = getPendingMessages(id);
		
		// Add the message to the list.
		Message message = new Message(msg, System.currentTimeMillis());
		pendingMessages.add(message);
	}

	private synchronized List<Message> getPendingMessages(String ip) {
		List<Message> pendingMessages = m_messages.get(ip);
		if (pendingMessages == null) {
			pendingMessages = new ArrayList<Message>();
			m_messages.put(ip, pendingMessages);
		}
		return pendingMessages;
	}

	private String getKeyIdentifier(SelectionKey selKey) {
		// Get hostname and port of key's socket.
		SocketChannel socketChannel = (SocketChannel) selKey.channel();
		Socket socket = socketChannel.socket();
		InetAddress address = socketChannel.socket().getInetAddress();
		String hostName = address.getHostAddress();
		int port = socket.getPort();
		
		// Build identifier.
		StringBuilder sb = new StringBuilder();
		sb.append(hostName);
		sb.append(":");
		sb.append(port);
		return sb.toString();
	}
	
	public synchronized List<Message> getMessages(SelectionKey selKey) {
		// Get the IP address identifying the selection key.
		String ip = getKeyIdentifier(selKey);
		
		// Get the list of pending messages for the client identified by selKey.
		List<Message> pendingMessages = m_messages.get(ip);
		if (pendingMessages != null && !pendingMessages.isEmpty()) {
			m_messages.remove(pendingMessages);
			return pendingMessages;
		}
		
		return null;
	}
}
