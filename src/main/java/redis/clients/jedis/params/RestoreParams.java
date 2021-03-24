package redis.clients.jedis.params;

import static redis.clients.jedis.Protocol.Keyword.ABSTTL;
import static redis.clients.jedis.Protocol.Keyword.FREQ;
import static redis.clients.jedis.Protocol.Keyword.IDLETIME;
import static redis.clients.jedis.Protocol.Keyword.REPLACE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.Protocol;

public class RestoreParams extends Params {

  private boolean replace;

  private boolean absTtl;

  private Long idleTime;

  private Long frequency;

  public static RestoreParams restoreParams() {
    return new RestoreParams();
  }

  public RestoreParams replace() {
    this.replace = true;
    return this;
  }

  public RestoreParams absTtl() {
    this.absTtl = true;
    return this;
  }

  public RestoreParams idleTime(long idleTime) {
    this.idleTime = idleTime;
    return this;
  }

  public RestoreParams frequency(long frequency) {
    this.frequency = frequency;
    return this;
  }

  public byte[][] getByteParams(byte[] key, byte[]... args) {
    List<byte[]> byteParams = new ArrayList<>();
    byteParams.add(key);
    Collections.addAll(byteParams, args);

    if (replace) {
      byteParams.add(REPLACE.getRaw());
    }

    if (absTtl) {
      byteParams.add(ABSTTL.getRaw());
    }

    if (idleTime != null) {
      byteParams.add(IDLETIME.getRaw());
      byteParams.add(Protocol.toByteArray(idleTime));
    }

    if (frequency != null) {
      byteParams.add(FREQ.getRaw());
      byteParams.add(Protocol.toByteArray(frequency));
    }
    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
