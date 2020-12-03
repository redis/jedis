package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

public class LPosParams extends Params {

  private static final String RANK = "RANK";
  private static final String MAXLEN = "MAXLEN";
  
  public static LPosParams lPosParams() {
    return new LPosParams();
  }

  public LPosParams rank(int rank) {
    addParam(RANK, rank);
    return this;
  }
  
  public LPosParams maxlen(int maxLen) {
    addParam(MAXLEN, maxLen);
    return this;
  }

  public byte[][] getByteParams(byte[]... args) {
    ArrayList<byte[]> byteParams = new ArrayList<>();
    Collections.addAll(byteParams, args);

    if (contains(RANK)) {
      byteParams.add(SafeEncoder.encode(RANK));
      byteParams.add(Protocol.toByteArray((int) getParam(RANK)));
    }

    if (contains(MAXLEN)) {
      byteParams.add(SafeEncoder.encode(MAXLEN));
      byteParams.add(Protocol.toByteArray((int) getParam(MAXLEN)));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }

}
