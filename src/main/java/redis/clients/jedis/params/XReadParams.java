package redis.clients.jedis.params;

public class XReadParams extends Params {

  private static final String COUNT = "COUNT";
  private static final String BLOCK = "BLOCK";

  public static XReadParams xReadParams() {
    return new XReadParams();
  }

  public XReadParams count(int count) {
    addParam(COUNT, count);
    return this;
  }

  public XReadParams block(int block) {
    addParam(BLOCK, block);
    return this;
  }

  public boolean hasBlock() {
    return super.contains(BLOCK);
  }
}
