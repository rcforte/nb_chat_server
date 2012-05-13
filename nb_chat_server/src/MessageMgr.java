import java.net.InetAddress;
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
			Iterator<Message> it = messages.iterator();
			while (it.hasNext()) {
				Message message = it.next();
				if (message.isOld()) {
					System.out.println("removing: " + message.content());
					it.remove();
				}
			}
			
			// Mark the map entries for removal.
			if (messages.isEmpty()) {
				toBeRemoved.add(entry.getKey());
			}
		}
		
		// Remove entries marked.
		if (!toBeRemoved.isEmpty()) {
			for (String key : toBeRemoved) {
				m_messages.remove(key);
			}
		}
	}
	
	public void addMessage(SelectionKey selKey, String msg) {
		// Get the IP address identifying the selection key.
		String ip = getIPAddress(selKey);
		
		// Get the list of pending messages. Create a new list if needed.
		List<Message> pendingMessages = getPendingMessages(ip);
		
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

	private String getIPAddress(SelectionKey selKey) {
		String ip;
		SocketChannel socketChannel = (SocketChannel) selKey.channel();
		InetAddress inetAddress = socketChannel.socket().getInetAddress();
		ip = inetAddress.getHostAddress() + ":" + socketChannel.socket().getPort();
		return ip;
	}
	
	public synchronized List<Message> getMessages(SelectionKey selKey) {
		// Get the IP address identifying the selection key.
		String ip = getIPAddress(selKey);
		
		// Get the list of pending messages for this IP.
		List<Message> pendingMessages = m_messages.get(ip);
		if (pendingMessages != null && !pendingMessages.isEmpty()) {
			m_messages.remove(pendingMessages);
			return pendingMessages;
		}
		return null;
	}
}
