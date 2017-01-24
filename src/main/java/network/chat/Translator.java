package network.chat;

/**
 * Created by Rafael on 1/24/2017.
 */
public interface Translator<From, To> {
  To from(From from);
  From to(To to);
}
