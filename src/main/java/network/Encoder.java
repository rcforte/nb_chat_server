package network;

import java.util.List;

/**
 * Created by Rafael on 1/20/2017.
 */
public interface Encoder<T> {
  List<T> decode(byte[] bytes);

  byte[] encode(T message);
}
