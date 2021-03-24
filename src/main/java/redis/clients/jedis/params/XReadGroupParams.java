package redis.clients.jedis.params;

public class XReadGroupParams extends Params {

  private static final String COUNT = "COUNT";
  private static final String BLOCK = "BLOCK";
  private static final String NOACK = "NOACK";

  public static XReadGroupParams xReadGroupParams() {
    return new XReadGroupParams();
  }

  public XReadGroupParams count(int count) {
    addParam(COUNT, count);
    return this;
  }

  public XReadGroupParams block(int block) {
    addParam(BLOCK, block);
    return this;
  }

  public XReadGroupParams noAck() {
    addParam(NOACK);
    return this;
  }

  public boolean hasBlock() {
    return super.contains(BLOCK);
  }
}
