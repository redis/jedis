package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.ArrayList;
import java.util.List;

public class ConnectionBrokenDeterminer {
  private List<ConnectionBrokenPattern> filters;

  public ConnectionBrokenDeterminer() {
    filters = new ArrayList<ConnectionBrokenPattern>();

    // applies default filter
    filters.add(new JedisConnectionExceptionAsConnectionBroken());
  }

  public void addPattern(final ConnectionBrokenPattern pattern) {
    filters.add(pattern);
  }

  public boolean determine(final RuntimeException exc) {
    for (ConnectionBrokenPattern filter : filters) {
      if (filter.determine(exc)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Treats JedisConnectionException as broken connection
   */
  private static class JedisConnectionExceptionAsConnectionBroken
      implements ConnectionBrokenPattern {
    @Override public boolean determine(final RuntimeException exc) {
      if (exc instanceof JedisConnectionException) {
        return true;
      }
      return false;
    }
  }
}
