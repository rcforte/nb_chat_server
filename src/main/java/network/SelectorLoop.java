package network;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * Created by Rafael on 1/16/2017.
 */
class SelectorLoop {

  private static final Logger logger = Logger.getLogger(SelectorLoop.class);
  private final Selector selector;
  private final SelectorListener selectorListener;
  private final Handler handler;
  private volatile boolean stopped;

  public SelectorLoop(Selector selector, SelectorListener selectorListener, Handler handler) {
    this.selector = selector;
    this.selectorListener = selectorListener;
    this.handler = handler;
  }

  public void stop() {
    this.stopped = true;
    this.selector.wakeup();
  }

  void start() {
    while (!stopped) {
      try {
        select();
      } catch (Exception e) {
        logger.error("error during select operation", e);
      }
    }

    logger.info("stopping selector...");
    try {
      selector.close();
    } catch (IOException e) {
      logger.error("Error closing selector", e);
    }

    selectorListener.onClosed();
  }

  void select() throws IOException {
    selectorListener.onBeforeSelect();

    int n = selector.select(100L);
    if (n == 0) {
      return;
    }

    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
    while (it.hasNext()) {
      SelectionKey key = it.next();

      if (key.isValid() && key.isAcceptable()) {
        handler.handleAccept(key);
      } else if (key.isValid() && key.isConnectable()) {
        handler.handleConnect(key);
      } else if (key.isValid() && key.isReadable()) {
        handler.handleRead(key);
      } else if (key.isValid() && key.isWritable()) {
        handler.handleWrite(key);
      }

      it.remove();
    }
  }
}
