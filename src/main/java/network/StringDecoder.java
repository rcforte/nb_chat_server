package network;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.function.Function;

/**
 * Created by Rafael on 1/20/2017.
 */
public class StringDecoder implements Function<byte[], List<String>> {
  private final String separator;
  private final StringBuilder buffer = new StringBuilder();

  public StringDecoder(String separator) {
    this.separator = separator;
  }

  @Override
  public List<String> apply(byte[] bytes) {
    buffer.append(new String(bytes));

    List<String> strs = Lists.newArrayList();
    String content = buffer.toString();
    String[] parts = content.split(separator);
    boolean isComplete = content.endsWith(separator);

    if (isComplete) {
      for (int i = 0; i < parts.length; i++) {
        strs.add(parts[i]);
      }
      buffer.delete(0, buffer.length());
    } else {
      for (int i = 0; i < parts.length - 1; i++) {
        strs.add(parts[i]);
      }
      buffer.delete(0, buffer.length())
          .append(parts[parts.length - 1]);
    }

    return strs;
  }
}
