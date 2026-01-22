package redis.clients.jedis;

import java.util.*;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.RediSearchUtil;
import redis.clients.jedis.util.JedisClusterCRC16;

public class CommandArguments implements Iterable<Rawable> {

  /**
   * Default initial capacity for the keys list. Most Redis commands have 1-3 keys,
   * so a small initial capacity avoids reallocations for common cases.
   */
  private static final int DEFAULT_KEYS_CAPACITY = 4;

  private CommandKeyArgumentPreProcessor keyPreProc = null;
  private final ArrayList<Rawable> args;

  /**
   * Pre-allocated list for storing keys. Using ArrayList directly avoids the
   * memory reallocation overhead of transitioning from emptyList -> singletonList -> ArrayList.
   */
  private final ArrayList<Object> keys;

  /**
   * Cached hash slots computed from keys. Null indicates the cache is invalid
   * and needs to be recomputed. The cache is invalidated when keys are added.
   */
  private Set<Integer> cachedHashSlots;

  private boolean blocking;

  private CommandArguments() {
    throw new InstantiationError();
  }

  public CommandArguments(ProtocolCommand command) {
    args = new ArrayList<>();
    args.add(command);

    keys = new ArrayList<>(DEFAULT_KEYS_CAPACITY);
    cachedHashSlots = null;
  }

  public ProtocolCommand getCommand() {
    return (ProtocolCommand) args.get(0);
  }

  @Experimental
  void setKeyArgumentPreProcessor(CommandKeyArgumentPreProcessor keyPreProcessor) {
    this.keyPreProc = keyPreProcessor;
  }

  public CommandArguments add(Rawable arg) {
    args.add(arg);
    return this;
  }

  public CommandArguments add(byte[] arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(boolean arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(int arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(long arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(double arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(String arg) {
    return add(RawableFactory.from(arg));
  }

  public CommandArguments add(Object arg) {
    if (arg == null) {
      throw new IllegalArgumentException("null is not a valid argument.");
    } else if (arg instanceof Rawable) {
      args.add((Rawable) arg);
    } else if (arg instanceof byte[]) {
      args.add(RawableFactory.from((byte[]) arg));
    } else if (arg instanceof Boolean) {
      args.add(RawableFactory.from((Boolean) arg));
    } else if (arg instanceof Integer) {
      args.add(RawableFactory.from((Integer) arg));
    } else if (arg instanceof Long) {
      args.add(RawableFactory.from((Long) arg));
    } else if (arg instanceof Double) {
      args.add(RawableFactory.from((Double) arg));
    } else if (arg instanceof float[]) {
      args.add(RawableFactory.from(RediSearchUtil.toByteArray((float[]) arg)));
    } else if (arg instanceof String) {
      args.add(RawableFactory.from((String) arg));
    } else if (arg instanceof GeoCoordinate) {
      GeoCoordinate geo = (GeoCoordinate) arg;
      args.add(RawableFactory.from(geo.getLongitude() + "," + geo.getLatitude()));
    } else {
      args.add(RawableFactory.from(String.valueOf(arg)));
    }
    return this;
  }

  public CommandArguments addObjects(Object... args) {
    for (Object arg : args) {
      add(arg);
    }
    return this;
  }

  public CommandArguments addObjects(Collection args) {
    args.forEach(arg -> add(arg));
    return this;
  }

  public CommandArguments key(Object key) {
    if (keyPreProc != null) {
      key = keyPreProc.actualKey(key);
    }

    if (key instanceof Rawable) {
      Rawable raw = (Rawable) key;
      args.add(raw);
    } else if (key instanceof byte[]) {
      byte[] raw = (byte[]) key;
      args.add(RawableFactory.from(raw));
    } else if (key instanceof String) {
      String raw = (String) key;
      args.add(RawableFactory.from(raw));
    } else {
      throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
    }

    addHashSlotKey(key);

    return this;
  }

  protected final CommandArguments addHashSlotKey(Object key) {
    keys.add(key);
    // Invalidate cached hash slots since keys have changed
    cachedHashSlots = null;
    return this;
  }

  public final CommandArguments keys(Object... keys) {
    Arrays.stream(keys).forEach(this::key);
    return this;
  }

  public final CommandArguments keys(Collection keys) {
    keys.forEach(this::key);
    return this;
  }

  public final CommandArguments addParams(IParams params) {
    params.addParams(this);
    return this;
  }

  protected final CommandArguments addHashSlotKeys(byte[]... keys) {
    for (byte[] key : keys) {
      addHashSlotKey(key);
    }
    return this;
  }

  protected final CommandArguments addHashSlotKeys(String... keys) {
    for (String key : keys) {
      addHashSlotKey(key);
    }
    return this;
  }

  public int size() {
    return args.size();
  }

  /**
   * Get the argument at the specified index.
   * @param index the index of the argument to retrieve (0-based, where 0 is the command itself)
   * @return the Rawable argument at the specified index
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public Rawable get(int index) {
    return args.get(index);
  }

  @Override
  public Iterator<Rawable> iterator() {
    return args.iterator();
  }

  @Internal
  public List<Object> getKeys() {
    return keys;
  }

  @Internal
  public Set<Integer> getKeyHashSlots() {
    // Return cached slots if available (cache is invalidated when keys are added)
    if (cachedHashSlots != null) {
      return cachedHashSlots;
    }

    // Compute hash slots and cache the result
    Set<Integer> slots = new HashSet<>();
    for (Object key : keys) {
      if (key instanceof byte[]) {
        slots.add(JedisClusterCRC16.getSlot((byte[]) key));
      } else {
        slots.add(JedisClusterCRC16.getSlot((String) key));
      }
    }
    // Cache as unmodifiable set to prevent external modification
    cachedHashSlots = Collections.unmodifiableSet(slots);
    return cachedHashSlots;
  }

  /**
   * @return true if this command has no keys, false otherwise
   */
  public boolean isKeyless() {
    return keys.isEmpty();
  }

  public boolean isBlocking() {
    return blocking;
  }

  public CommandArguments blocking() {
    this.blocking = true;
    return this;
  }
}
