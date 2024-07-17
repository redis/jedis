package redis.clients.jedis.util;

import redis.clients.jedis.CommandKeyArgumentPreProcessor;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;

@Experimental
public class PrefixedKeyArgumentPreProcessor implements CommandKeyArgumentPreProcessor {

  private final byte[] prefixBytes;
  private final String prefixString;

  public PrefixedKeyArgumentPreProcessor(String prefix) {
    this(prefix, SafeEncoder.encode(prefix));
  }

  public PrefixedKeyArgumentPreProcessor(String prefixString, byte[] prefixBytes) {
    this.prefixString = prefixString;
    this.prefixBytes = prefixBytes;
  }

  @Override
  public Object actualKey(Object paramKey) {
    return prefixKey(paramKey, prefixString, prefixBytes);
  }

  private static Object prefixKey(Object key, String prefixString, byte[] prefixBytes) {
    if (key instanceof Rawable) {
      byte[] raw = ((Rawable) key).getRaw();
      return RawableFactory.from(prefixKeyWithBytes(raw, prefixBytes));
    } else if (key instanceof byte[]) {
      return prefixKeyWithBytes((byte[]) key, prefixBytes);
    } else if (key instanceof String) {
      String raw = (String) key;
      return prefixString + raw;
    }
    throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
  }

  private static byte[] prefixKeyWithBytes(byte[] key, byte[] prefixBytes) {
    byte[] namespaced = new byte[prefixBytes.length + key.length];
    System.arraycopy(prefixBytes, 0, namespaced, 0, prefixBytes.length);
    System.arraycopy(key, 0, namespaced, prefixBytes.length, key.length);
    return namespaced;
  }
}
