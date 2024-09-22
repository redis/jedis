package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.ClientType;
import redis.clients.jedis.util.KeyValue;

public class ClientKillParams implements IParams {

  public static enum SkipMe {
    YES, NO;
  }

  private final ArrayList<KeyValue<Keyword, Object>> params = new ArrayList<>();

  public ClientKillParams() {
  }

  public static ClientKillParams clientKillParams() {
    return new ClientKillParams();
  }

  private ClientKillParams addParam(Keyword key, Object value) {
    params.add(KeyValue.of(key, value));
    return this;
  }

  public ClientKillParams id(String clientId) {
    return addParam(Keyword.ID, clientId);
  }

  public ClientKillParams id(byte[] clientId) {
    return addParam(Keyword.ID, clientId);
  }

  public ClientKillParams type(ClientType type) {
    return addParam(Keyword.TYPE, type);
  }

  public ClientKillParams addr(String ipPort) {
    return addParam(Keyword.ADDR, ipPort);
  }

  public ClientKillParams addr(byte[] ipPort) {
    return addParam(Keyword.ADDR, ipPort);
  }

  public ClientKillParams addr(String ip, int port) {
    return addParam(Keyword.ADDR, ip + ':' + port);
  }

  public ClientKillParams skipMe(SkipMe skipMe) {
    return addParam(Keyword.SKIPME, skipMe);
  }

  public ClientKillParams user(String username) {
    return addParam(Keyword.USER, username);
  }

  public ClientKillParams laddr(String ipPort) {
    return addParam(Keyword.LADDR, ipPort);
  }

  public ClientKillParams laddr(String ip, int port) {
    return addParam(Keyword.LADDR, ip + ':' + port);
  }

  /**
   * Kill clients older than {@code maxAge} seconds.
   *
   * @param maxAge Clients older than this number of seconds will be killed.
   * @return The {@code ClientKillParams} instance, for call chaining.
   */
  public ClientKillParams maxAge(long maxAge) {
    return addParam(Keyword.MAXAGE, maxAge);
  }

  @Override
  public void addParams(CommandArguments args) {
    params.forEach(kv -> args.add(kv.getKey()).add(kv.getValue()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientKillParams that = (ClientKillParams) o;
    return Objects.equals(params, that.params);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params);
  }
}
