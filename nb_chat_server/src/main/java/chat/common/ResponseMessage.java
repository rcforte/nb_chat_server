package chat.common;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.List;

/**
 * Created by Rafael on 1/15/2017.
 */
public class ResponseMessage {

    public static ResponseMessage fromBytes(byte[] bytes) {
        String json = new String(bytes);
        Gson gson = new Gson();
        return gson.fromJson(json, ResponseMessage.class);
    }

    private List<String> rooms = Lists.newArrayList();
    private String correlationId;

    public List<String> getRooms() {
        return rooms;
    }

    public void setRooms(List<String> rooms) {
        this.rooms = rooms;
    }

    public void addRoom(String room) {
        rooms.add(room);
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
