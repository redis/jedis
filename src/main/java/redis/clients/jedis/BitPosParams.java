package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BitPosParams {
  private List<byte[]> params = new ArrayList<byte[]>();

  protected BitPosParams() {
  }

  public BitPosParams(long start) {
    params.add(Protocol.toByteArray(start));
  }

  public BitPosParams(long start, long end) {
    this(start);

    params.add(Protocol.toByteArray(end));
  }

  public Collection<byte[]> getParams() {
    return Collections.unmodifiableCollection(params);
  }
}
