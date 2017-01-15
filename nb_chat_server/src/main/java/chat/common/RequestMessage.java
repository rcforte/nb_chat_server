package chat.common;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by Rafael on 1/15/2017.
 */
public class RequestMessage {

    public static RequestMessage fromBytes(byte[] data) {
        String json = new String(data);
        Gson gson = new Gson();
        return gson.fromJson(json, RequestMessage.class);
    }

    private List<String> rooms = Lists.newArrayList();
    private RequestMessageType requestMessageType;
    private String correlationId;

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    public List<String> getRooms() {
        return rooms;
    }

    public void addChatRoom(String room) {
        rooms.add(room);
    }

    public RequestMessageType getRequestMessageType() {
        return requestMessageType;
    }

    public void setRequestMessageType(RequestMessageType requestMessageType) {
        this.requestMessageType = requestMessageType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    // Todo: extract logic to encoder
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
