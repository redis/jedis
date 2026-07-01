package redis.clients.jedis;

import java.util.concurrent.ConcurrentLinkedDeque;

class AdvancedTimeoutSupplier implements TimeoutSupplier {

  ConcurrentLinkedDeque<TimeoutCard> stack = new ConcurrentLinkedDeque<>();
  final DefaultTimeoutCard defaultCard;

  /**
   * Cached effective (top) card. {@link #get()} runs on every command, so resolving the timeout
   * costs a single volatile read plus a validity check instead of walking the deque; the deque is
   * only touched on {@link #push}/{@link #remove} and on the slow path when the cached card has
   * expired by time. {@code volatile} because push/remove may run on a maintenance thread while
   * {@code get()} runs on the command thread.
   */
  private volatile TimeoutCard top;

  /**
   * Create a new TimeoutSupplier with the default timeout.
   * <p>
   * Initial timeout values provided here will be applied as default and will always be valid even
   * if there are no other timeout info pushed in.
   * @param defaultCard the never-expiring floor card carrying the configured timeouts
   */
  AdvancedTimeoutSupplier(DefaultTimeoutCard defaultCard) {
    this.defaultCard = defaultCard;
    stack.push(defaultCard);
    this.top = defaultCard;
  }

  public TimeoutCard get() {
    TimeoutCard current = top;
    // defaultCard.isValid() is a constant true (no clock read); only a live pushed card reads the
    // clock here. The cached top changes only on push/remove or when it expires — not per command.
    if (current == defaultCard || current.isValid()) {
      return current;
    }
    return advance();
  }

  /** Slow path: the cached top expired by time. Drop expired cards off the deque and recache. */
  private TimeoutCard advance() {
    TimeoutCard current = stack.peek();
    while (!current.isValid()) {
      stack.remove(current);
      current = stack.peek();
    }
    top = current;
    return current;
  }

  public TimeoutCard push(TimeoutInfo info) {
    TimeoutCard card = new TimeoutCard(info);
    stack.push(card);
    top = stack.peek(); // newest push becomes the effective card
    return card;
  }

  public void remove(TimeoutCard card) {
    stack.remove(card);
    top = stack.peek(); // never null: defaultCard is the permanent floor
  }

  public void setDefaults(int timeout, int blockingTimeout) {
    defaultCard.set(timeout, blockingTimeout);
  }

  public TimeoutCard getDefaults() {
    return defaultCard;
  }
}
