package network;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Rafael on 1/20/2017.
 */
public class TokenMessageEncoder implements MessageEncoder<String> {
    private final String token;
    private final StringBuilder buffer = new StringBuilder();

    public TokenMessageEncoder(String token) {
        this.token = token;
    }

    public List<String> decode(byte[] bytes) {
        buffer.append(new String(bytes));

        List<String> messages = Lists.newArrayList();
        String allContent = buffer.toString();
        String[] parts = allContent.split(token);
        boolean isCompleteMessage = allContent.endsWith(token);

        if (isCompleteMessage) {
            for (int i = 0; i < parts.length; i++) {
                messages.add(parts[i]);
            }
            buffer.delete(0, buffer.length());
        } else {
            for (int i = 0; i < parts.length - 1; i++) {
                messages.add(parts[i]);
            }
            buffer.delete(0, buffer.length());
            buffer.append(parts[parts.length - 1]);
        }

        return messages;
    }

    public byte[] encode(String message) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(message);
        buffer.append(token);
        return buffer.toString().getBytes();
    }

    public static void main(String[] args) {
        String test = "Test\nMeme\nTriple";
        String[] parts = test.split("\n");
        System.out.println(Arrays.toString(parts));
    }
}
