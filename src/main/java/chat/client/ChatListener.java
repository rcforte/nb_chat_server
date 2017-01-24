package chat.client;

import chat.common.Message;

public interface ChatListener {
  void onChatEvent(Message msg);
}
