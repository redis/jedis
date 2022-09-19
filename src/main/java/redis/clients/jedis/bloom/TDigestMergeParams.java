package redis.clients.jedis.bloom;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.COMPRESSION;
import static redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword.OVERRIDE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class TDigestMergeParams implements IParams {

  private Integer compression;
  private boolean override = false;

  public static TDigestMergeParams mergeParams() {
    return new TDigestMergeParams();
  }

  public TDigestMergeParams compression(int compression) {
    this.compression = compression;
    return this;
  }

  public TDigestMergeParams override() {
    this.override = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (compression != null) {
      args.add(COMPRESSION).add(toByteArray(compression));
    }
    if (override) {
      args.add(OVERRIDE);
    }
  }
}
