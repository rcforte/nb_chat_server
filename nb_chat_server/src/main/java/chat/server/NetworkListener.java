package chat.server;

/**
 * Created by Rafael on 1/15/2017.
 */
public interface NetworkListener {
    void onEvent(NetworkEvent networkEvent);
}
