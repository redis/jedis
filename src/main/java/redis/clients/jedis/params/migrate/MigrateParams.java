package redis.clients.jedis.params.migrate;

import java.util.ArrayList;

import redis.clients.jedis.params.Params;
import redis.clients.util.SafeEncoder;

public class MigrateParams extends Params {

  private static final String COPY = "copy";
  private static final String REPLACE = "replace";
  private static final String KEYS = "keys";

  public MigrateParams() {
  }

  public static MigrateParams migrateParms() {
    return new MigrateParams();
  }

  public MigrateParams copy() {
    addParam(COPY);
    return this;
  }

  public MigrateParams replace() {
    addParam(REPLACE);
    return this;
  }

  public byte[][] getMigrateByteParams(byte[] host, byte[] port, byte[] destinationDb,
      byte[] timeout, byte[]... keys) {
    ArrayList<byte[]> byteParams = new ArrayList<byte[]>();

    byteParams.add(host);
    byteParams.add(port);
    byteParams.add(SafeEncoder.encode(""));
    byteParams.add(destinationDb);
    byteParams.add(timeout);

    if (contains(COPY)) {
      byteParams.add(SafeEncoder.encode(REPLACE));
    }

    if (contains(REPLACE)) {
      byteParams.add(SafeEncoder.encode(REPLACE));
    }

    if (keys.length > 0) {
      byteParams.add(SafeEncoder.encode(KEYS));
    }

    for (byte[] arg : keys) {
      byteParams.add(arg);
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

  public byte[][] getRestoreByteParams(byte[] key, byte[] ttl, byte[] serializedValue) {
    ArrayList<byte[]> byteParams = new ArrayList<byte[]>();

    byteParams.add(key);
    byteParams.add(ttl);
    byteParams.add(serializedValue);

    if (contains(COPY)) {
      byteParams.add(SafeEncoder.encode(REPLACE));
    }

    if (contains(REPLACE)) {
      byteParams.add(SafeEncoder.encode(REPLACE));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
