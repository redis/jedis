package redis.clients.jedis.params;

public class ClientKillParams extends Params {

  private static final String ID = "ID";
  private static final String TYPE = "TYPE";
  private static final String ADDR = "ADDR";
  private static final String SKIPME = "SKIPME";

  public static enum Type {
    NORMAL, MASTER, SLAVE, PUBSUB;
  }

  public static enum SkipMe {
    YES, NO;
  }

  public ClientKillParams() {
  }

  public static ClientKillParams clientKillParams() {
    return new ClientKillParams();
  }

  public ClientKillParams id(String clientId) {
    addParam(ID, clientId);
    return this;
  }

  public ClientKillParams id(byte[] clientId) {
    addParam(ID, clientId);
    return this;
  }

  public ClientKillParams type(Type type) {
    addParam(TYPE, type);
    return this;
  }

  public ClientKillParams addr(String ipPort) {
    addParam(ADDR, ipPort);
    return this;
  }

  public ClientKillParams addr(byte[] ipPort) {
    addParam(ADDR, ipPort);
    return this;
  }

  public ClientKillParams addr(String ip, int port) {
    addParam(ADDR, ip + ':' + port);
    return this;
  }

  public ClientKillParams skipMe(SkipMe skipMe) {
    addParam(SKIPME, skipMe);
    return this;
  }

}
