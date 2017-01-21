package chat.client;

import chat.common.ResponseMessage;

public interface ChatListener {
	void onChatEvent(ResponseMessage responseMessage);
}
