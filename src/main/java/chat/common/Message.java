package chat.common;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by Rafael on 1/15/2017.
 */
public class Message {
  private static final Gson gson = new Gson();

  public static final Message from(String data) {
    return gson.fromJson(new String(data), Message.class);
  }

  public static final Message message(MessageType type, String corrId) {
    return new Message(type, corrId);
  }

  public static final Message message(MessageType type) {
    return new Message(type, null);
  }

  private final MessageType type;
  private final String correlationId;
  private final Map<String, List<String>> attrs = Maps.newHashMap();

  public Message(MessageType type, String correlationId) {
    this.type = type;
    this.correlationId = correlationId;
  }

  public Message(MessageType type) {
    this(type, null);
  }

  public MessageType getType() {
    return type;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public List<String> get(String name) {
    return attrs.get(name);
  }

  public String getFirst(String name) {
    return attrs.get(name).get(0);
  }

  public Message with(String name, String value) {
    attrs.put(name, newArrayList(value));
    return this;
  }

  public Message with(String name, List<String> values) {
    values.stream().forEach(val -> with(name, val));
    return this;
  }

  public MessageType type() {
    return type;
  }

  public String corrId() {
    return getCorrelationId();
  }

  public String json() {
    return gson.toJson(this);
  }
}
