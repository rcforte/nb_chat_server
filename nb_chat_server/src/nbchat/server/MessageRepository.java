package nbchat.server;

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

public class MessageRepository {
	private static final long CLEANUP_INTERVAL = 50000L;

	private final Map<String, List<Message>> m_storedMessages = Collections.synchronizedMap(new LinkedHashMap<String, List<Message>>());
	private final Timer m_cleanupTimer = new Timer();
	private final TimerTask m_cleanupTask = new TimerTask() {
		@Override
		public void run() {
			handleCleanup();
		}
	};

	public MessageRepository() {
		m_cleanupTimer.schedule(m_cleanupTask, 0L, CLEANUP_INTERVAL);
	}

	private synchronized void handleCleanup() {
		List<String> toBeRemoved = new ArrayList<String>();
		for (Map.Entry<String, List<Message>> entry : m_storedMessages.entrySet()) {
			// Remove messages.
			List<Message> messages = entry.getValue();
			for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
				Message message = it.next();
				if (message.isOld()) {
					it.remove();
				}
			}

			// Remove empty entry.
			if (messages.isEmpty()) {
				toBeRemoved.add(entry.getKey());
			}
		}
		if (!toBeRemoved.isEmpty()) {
			for (String key : toBeRemoved) {
				m_storedMessages.remove(key);
			}
		}
	}

	public void addMessage(SelectionKey selKey, String msg) {
		// Add to pending.
		String id = getKeyIdentifier(selKey);
		List<Message> pendingMessages = getPendingMessages(id);
		Message message = new Message(msg, System.currentTimeMillis());
		pendingMessages.add(message);
	}

	private synchronized List<Message> getPendingMessages(String ip) {
		List<Message> pendingMessages = m_storedMessages.get(ip);
		if (pendingMessages == null) {
			pendingMessages = new ArrayList<Message>();
			m_storedMessages.put(ip, pendingMessages);
		}
		return pendingMessages;
	}

	private String getKeyIdentifier(SelectionKey selKey) {
		// Get client info.
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
		// Get pending messages.
		String id = getKeyIdentifier(selKey);
		List<Message> pendingMessages = m_storedMessages.get(id);
		if (pendingMessages != null && !pendingMessages.isEmpty()) {
			m_storedMessages.remove(pendingMessages);
			return pendingMessages;
		}
		return null;
	}
}
