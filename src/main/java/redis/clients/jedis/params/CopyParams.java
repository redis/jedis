package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.DB;
import static redis.clients.jedis.Protocol.Keyword.REPLACE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;

public class CopyParams extends Params {

  private Long destinationDb;

  private boolean replace;

  public static CopyParams copyParams() {
    return new CopyParams();
  }

  public CopyParams destinationDb(long destinationDb) {
    this.destinationDb = destinationDb;
    return this;
  }

  public CopyParams replace() {
    this.replace = true;
    return this;
  }

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    List<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);
    Collections.addAll(byteParams, args);

    if (destinationDb != null) {
      byteParams.add(DB.getRaw());
      byteParams.add(Protocol.toByteArray(destinationDb));
    }

    if (replace) {
      byteParams.add(REPLACE.getRaw());
    }
    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
