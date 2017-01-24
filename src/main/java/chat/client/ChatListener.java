package chat.client;

import chat.common.Response;

public interface ChatListener {
  void onChatEvent(Response response);
}
