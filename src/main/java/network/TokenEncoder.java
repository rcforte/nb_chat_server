package network;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Rafael on 1/20/2017.
 */
public class TokenEncoder implements Encoder<String> {
  private static final Logger logger = Logger.getLogger(TokenEncoder.class);
  private final String token;
  private final StringBuilder buffer = new StringBuilder();

  public TokenEncoder(String token) {
    this.token = token;
  }

  public static void main(String[] args) {
    String test = "Test\nMeme\nTriple";
    String[] parts = test.split("\n");
    System.out.println(Arrays.toString(parts));
  }

  public List<String> decode(byte[] bytes) {
    buffer.append(new String(bytes));

    List<String> messages = Lists.newArrayList();
    String allContent = buffer.toString();
    String[] parts = allContent.split(token);
    logger.info("message parts: " + Arrays.toString(parts));
    boolean isCompleteMessage = allContent.endsWith(token);

    if (isCompleteMessage) {
      logger.info("all messages are complete");
      for (int i = 0; i < parts.length; i++) {
        messages.add(parts[i]);
      }
      buffer.delete(0, buffer.length());
    } else {
      logger.info("the last message is incomplete");
      for (int i = 0; i < parts.length - 1; i++) {
        logger.info("adding complete message to result");
        messages.add(parts[i]);
      }
      buffer.delete(0, buffer.length());
      logger.info("keeping incomplete message in buffer");
      buffer.append(parts[parts.length - 1]);
      logger.info("buffer: " + buffer.toString());
    }

    return messages;
  }

  public byte[] encode(String message) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(message);
    buffer.append(token);
    return buffer.toString().getBytes();
  }
}
