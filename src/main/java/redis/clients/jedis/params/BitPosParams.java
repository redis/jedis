package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

public class BitPosParams implements IParams {
  private List<byte[]> params = new ArrayList<>();

  public BitPosParams() {
  }

  public BitPosParams(long start) {
    params.add(Protocol.toByteArray(start));
  }

  public BitPosParams(long start, long end) {
    this(start);

    params.add(Protocol.toByteArray(end));
  }

  @Override
  public void addParams(CommandArguments args) {
    for (byte[] param : params) {
      args.add(param);
    }
  }
}
