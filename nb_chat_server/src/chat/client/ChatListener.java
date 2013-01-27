package chat.client;

import java.util.List;

public interface ChatListener {
	public void onConnected();

	public void onChatRooms(List<String> rooms);

	public void onFail(String reason);
}
