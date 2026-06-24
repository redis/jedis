package redis.clients.jedis;

import java.util.concurrent.ConcurrentLinkedDeque;

class AdvancedTimeoutSupplier implements TimeoutSupplier {

  ConcurrentLinkedDeque<TimeoutCard> stack = new ConcurrentLinkedDeque<>();

  /**
   * Create a new TimeoutSupplier with the default timeout.
   * <p>
   * Initial timeout values provided here will be applied as default and will always be valid even
   * if there are no other timeout info pushed in.
   * @param timeout the default timeout in milliseconds
   * @param blockingTimeout the default blocking timeout in milliseconds
   */
  AdvancedTimeoutSupplier(DefaultTimeoutCard defaultCard) {
    stack.push(defaultCard);
  }

  public TimeoutCard get() {
    TimeoutCard current = stack.peek();
    while (!current.isValid()) {
      stack.forEach(c -> c.readyConsume());
      stack.remove(current);
      current = stack.peek();
    }
    return current;
  }

  public TimeoutCard push(TimeoutInfo info) {
    TimeoutCard card = new TimeoutCard(info);
    stack.push(card);
    return card;
  }

  public void remove(TimeoutCard card) {
    stack.forEach(c -> c.readyConsume());
    stack.remove(card);
  }

}
