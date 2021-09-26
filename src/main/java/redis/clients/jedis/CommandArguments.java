package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Iterator;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.params.IParams;

public class CommandArguments implements Iterable<Rawable> {

  private final ArrayList<Rawable> args;

  private boolean blocking;

  private CommandArguments() {
    throw new InstantiationError();
  }

  public CommandArguments(ProtocolCommand command) {
    args = new ArrayList<>();
    args.add(command);
  }

  public ProtocolCommand getCommand() {
    return (ProtocolCommand) args.get(0);
  }
//
//  public boolean add(String string) {
//    return args.add(RawableFactory.from(string));
//  }
//
//  public boolean add(byte[] binary) {
//    return args.add(RawableFactory.from(binary));
//  }

//  public boolean addObject(Object arg) {
  public CommandArguments addObject(Object arg) {
    if (arg instanceof Rawable) {
      args.add((Rawable) arg);
    } else if (arg instanceof byte[]) {
//      this.add((byte[]) arg);
      args.add(RawableFactory.from((byte[]) arg));
    } else if (arg instanceof String) {
//      this.add((String) arg);
      args.add(RawableFactory.from((String) arg));
    } else {
//      throw new IllegalArgumentException("\"" + arg.toString() + "\" is not a valid argument.");
      args.add(RawableFactory.from(String.valueOf(arg)));
    }
//    return true;
    return this;
  }

  public CommandArguments addObjects(Object... args) {
    for (Object arg : args) {
      addObject(arg);
    }
    return this;
  }
//
//  public boolean addKey(String string) {
//    return this.add(string);
//  }
//
//  public boolean addKey(byte[] binary) {
//    return this.add(binary);
//  }

//  public boolean addKeyObject(Object arg) {
  public CommandArguments addKeyObject(Object key) {
    if (key instanceof Rawable) {
//      this.addKey(((Rawable) arg).getRaw());
      Rawable raw = (Rawable) key;
      processKey(raw.getRaw());
      args.add(raw);
    } else if (key instanceof byte[]) {
//      this.addKey((byte[]) arg);
      byte[] raw = (byte[]) key;
      processKey(raw);
      args.add(RawableFactory.from(raw));
    } else if (key instanceof String) {
//      this.addKey((String) arg);
      String raw = (String) key;
      processKey(raw);
      args.add(RawableFactory.from(raw));
    } else {
      throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
    }
//    return true;
    return this;
  }

  public CommandArguments addKeyObjects(Object... args) {
    for (Object arg : args) {
      addKeyObject(arg);
    }
    return this;
  }

  public CommandArguments addParams(IParams params) {
    params.addParams(this);
    return this;
  }

  protected void processKey(byte[] key) {
    // do nothing
  }

  protected void processKey(String key) {
    // do nothing
  }
//
//  public static CommandArguments of(ProtocolCommand command, Object... args) {
//    CommandArguments ca = new CommandArguments(command);
//    for (Object arg : args) {
//      ca.addObject(arg);
//    }
//    return ca;
//  }

  public int size() {
    return args.size();
  }

  @Override
  public Iterator<Rawable> iterator() {
    return args.iterator();
  }

  public boolean isBlocking() {
    return blocking;
  }

  public CommandArguments blocking() {
    this.blocking = true;
    return this;
  }
}
