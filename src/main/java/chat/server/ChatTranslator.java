package chat.server;

import chat.common.Message;
import network.StringDecoder;
import network.StringEncoder;
import network.Translator;

import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * Created by Rafael on 1/24/2017.
 */
public class ChatTranslator implements Translator<byte[], List<Message>> {
  public static ChatTranslator translator() {
    // from bytes to messages
    Function<List<String>, List<Message>> strToMsg = strs -> strs.stream().map(Message::from).collect(toList());
    Function<byte[], List<Message>> from = strToMsg.compose(new StringDecoder("\n"));

    // from messages to bytes
    Function<List<Message>, List<String>> msgToStr = msgs -> msgs.stream().map(Message::json).collect(toList());
    Function<List<Message>, byte[]> to = new StringEncoder("\n").compose(msgToStr);

    return new ChatTranslator(from, to);
  }

  private final Function<byte[], List<Message>> from;
  private final Function<List<Message>, byte[]> to;

  public ChatTranslator(Function<byte[], List<Message>> from, Function<List<Message>, byte[]> to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public List<Message> from(byte[] bytes) {
    return from.apply(bytes);
  }

  @Override
  public byte[] to(List<Message> msgs) {
    return to.apply(msgs);
  }
}
