package chat.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * Created by Rafael on 1/15/2017.
 */
public class Response {
  private static final Gson gson = new Gson();

  public static Response from(String json) {
    return gson.fromJson(json, Response.class);
  }

  public static Response response() {
    return new Response();
  }

  public static Response response(String corrId) {
    return new Response(corrId);
  }

  private final String correlationId;
  private final Map<String, List<String>> attributes = Maps.newHashMap();

  public Response(String correlationId) {
    this.correlationId = correlationId;
  }

  public Response() {
    this(null);
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public List<String> get(String name) {
    return attributes.get(name);
  }

  public Response with(String name, List<String> value) {
    attributes.put(name, value);
    return this;
  }

  public Response with(String name, String value) {
    return with(name, Lists.newArrayList(value));
  }

  public String json() {
    return gson.toJson(this);
  }

  public String corrId() {
    return getCorrelationId();
  }

}
