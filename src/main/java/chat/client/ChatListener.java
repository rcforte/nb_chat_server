package chat.client;

import chat.common.ResponseMessage;

import java.util.List;

public interface ChatListener {
	void onChatEvent(ResponseMessage responseMessage);
}
