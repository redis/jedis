package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.RediSearchUtil;

public class CommandArguments implements Iterable<Rawable> {

  private CommandKeyArgumentPreProcessor keyPreProc = null;
  private final ArrayList<Rawable> args;

  private List<Object> keys;

  private boolean blocking;

  private CommandArguments() {
    throw new InstantiationError();
  }

  public CommandArguments(ProtocolCommand command) {
    args = new ArrayList<>();
    args.add(command);

    keys = Collections.emptyList();
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
      processKey(raw.getRaw());
      args.add(raw);
    } else if (key instanceof byte[]) {
      byte[] raw = (byte[]) key;
      processKey(raw);
      args.add(RawableFactory.from(raw));
    } else if (key instanceof String) {
      String raw = (String) key;
      processKey(raw);
      args.add(RawableFactory.from(raw));
    } else {
      throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
    }

    addKeyInKeys(key);

    return this;
  }

  private void addKeyInKeys(Object key) {
    if (keys.isEmpty()) {
      keys = Collections.singletonList(key);
    } else if (keys.size() == 1) {
      List oldKeys = keys;
      keys = new ArrayList();
      keys.addAll(oldKeys);
      keys.add(key);
    } else {
      keys.add(key);
    }
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

  protected CommandArguments processKey(byte[] key) {
    // do nothing
    return this;
  }

  protected final CommandArguments processKeys(byte[]... keys) {
    for (byte[] key : keys) {
      processKey(key);
    }
    return this;
  }

  protected CommandArguments processKey(String key) {
    // do nothing
    return this;
  }

  protected final CommandArguments processKeys(String... keys) {
    for (String key : keys) {
      processKey(key);
    }
    return this;
  }

  public int size() {
    return args.size();
  }

  @Override
  public Iterator<Rawable> iterator() {
    return args.iterator();
  }

  @Internal
  public List<Object> getKeys() {
    return keys;
  }

  public boolean isBlocking() {
    return blocking;
  }

  public CommandArguments blocking() {
    this.blocking = true;
    return this;
  }
}
