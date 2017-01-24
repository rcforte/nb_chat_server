package network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import network.Encoder;

import java.util.List;

/**
 * Created by Rafael on 1/21/2017.
 */
public class JsonEncoder implements Encoder<ChatMessage> {
  private final Gson gson = new Gson();

  @Override
  public List<ChatMessage> decode(byte[] bytes) {
    return Lists.newArrayList(gson.fromJson(new String(bytes), ChatMessage.class));
  }

  @Override
  public byte[] encode(ChatMessage message) {
    return gson.toJson(message).getBytes();
  }
}
