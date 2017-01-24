package chat.common;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import java.util.Map;

/**
 * Created by Rafael on 1/15/2017.
 */
public class Request {
  private static final Gson gson = new Gson();

  public static final Request fromString(String data) {
    return gson.fromJson(new String(data), Request.class);
  }

  public static final String toJson(Request request) {
    return gson.toJson(request);
  }

  private final RequestType type;
  private final String correlationId;
  private final Map<String, String> attributes = Maps.newHashMap();

  public Request(RequestType type, String correlationId) {
    this.type = type;
    this.correlationId = correlationId;
  }

  public Request(RequestType type) {
    this(type, null);
  }


  public RequestType getType() {
    return type;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String get(String name) {
    return attributes.get(name);
  }

  public Request with(String name, String value) {
    attributes.put(name, value);
    return this;
  }

  public String corrId() {
    return getCorrelationId();
  }
}
