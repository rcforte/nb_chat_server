package chat.client;

import java.util.List;

class ChatEvent {
	public static enum Type {
		ON_CONNECT, ON_FAIL, ON_GET_ROOMS;
	}
	private final ChatEvent.Type m_type;
	private final String m_reason;
	private final List<String> m_chatRooms;
	
	public static ChatEvent newFailEvent(String reason) {
		return new ChatEvent(Type.ON_FAIL, null, reason);
	}

	public static ChatEvent newConnectEvent() {
		return new ChatEvent(Type.ON_CONNECT, null, null);
	}

	public static ChatEvent newGetChatRoomsEvent(List<String> chatRooms) {
		return new ChatEvent(Type.ON_GET_ROOMS, chatRooms, null);
	}
	
	public ChatEvent.Type getType() {
		return m_type;
	}

	public String getReason() {
		return m_reason;
	}

	private ChatEvent(Type type, List<String> chatRooms, String reason) {
		m_type = type;
		m_reason = reason;
		m_chatRooms = chatRooms;
	}

	public List<String> getChatRooms() {
		return m_chatRooms;
	}
}