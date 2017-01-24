package network.chat;

import com.google.common.collect.Lists;
import network.Encoder;
import network.TokenEncoder;

import java.util.List;

/**
 * Created by Rafael on 1/21/2017.
 */
public class ChatEncoder implements Encoder<ChatMessage> {
  private final TokenEncoder newLineEncoder = new TokenEncoder("\n");
  private final JsonEncoder jsonEncoder = new JsonEncoder();

  @Override
  public List<ChatMessage> decode(byte[] bytes) {
    List<ChatMessage> result = Lists.newArrayList();
    for (String json : newLineEncoder.decode(bytes)) {
      result.addAll(jsonEncoder.decode(json.getBytes()));
    }
    return result;
  }

  @Override
  public byte[] encode(ChatMessage chatMessage) {
    byte[] bytes = jsonEncoder.encode(chatMessage);
    return newLineEncoder.encode(new String(bytes));
  }

}
