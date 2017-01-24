package network;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Function;

/**
 * Created by Rafael on 1/24/2017.
 */
public class StringEncoder implements Function<List<String>, byte[]> {
  private final String separator;

  public StringEncoder(String separator) {
    this.separator = separator;
  }

  @Override
  public byte[] apply(List<String> strs) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      for (String str : strs) {
        buffer.write((str + separator).getBytes());
      }
      return buffer.toByteArray();
    } catch (Exception e) {
      return null;
    }
  }
}
