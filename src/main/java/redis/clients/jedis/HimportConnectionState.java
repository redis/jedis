package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Per-{@link Connection} HIMPORT bookkeeping: which fieldsets are prepared on this socket, and
 * which ones {@link HashImport#close()} has queued to be discarded before the connection's next
 * command. Pure state &mdash; it issues no commands; {@link Connection} performs the actual
 * {@code DISCARD} I/O.
 * <p>
 * {@code prepared} is single-owner: only the thread currently holding the connection reads or
 * writes it (prepare-before-use and the drain), so it needs no synchronization.
 * {@code pendingDiscard} is concurrent because {@code close()} enqueues onto it from a thread that
 * does not own the connection. {@code discardPending} makes the per-command probe a single volatile
 * read.
 */
class HimportConnectionState {

  private final Set<String> prepared = new HashSet<>();
  private final Queue<String> pendingDiscard = new ConcurrentLinkedQueue<>();
  private volatile boolean discardPending = false;

  boolean isPrepared(String fieldset) {
    return prepared.contains(fieldset);
  }

  void markPrepared(String fieldset) {
    prepared.add(fieldset);
  }

  void markForDiscard(String fieldset) {
    pendingDiscard.add(fieldset);
    discardPending = true;
  }

  /**
   * Drains the fieldsets queued for discard that are actually prepared on this connection (removing
   * them from the prepared set), for the caller to {@code DISCARD}.
   */
  List<String> drainDiscardable() {
    if (!discardPending) {
      return Collections.emptyList();
    }

    discardPending = false;
    List<String> discardable = new ArrayList<>();
    String fieldset;
    while ((fieldset = pendingDiscard.poll()) != null) {
      if (prepared.remove(fieldset)) {
        discardable.add(fieldset);
      }
    }
    return discardable;
  }

  /**
   * Drops all state without any discard: the socket was (re)connected or broken, so the server no
   * longer holds these fieldsets.
   */
  void forget() {
    prepared.clear();
    pendingDiscard.clear();
    discardPending = false;
  }
}
