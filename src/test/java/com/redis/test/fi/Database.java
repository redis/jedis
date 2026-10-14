package com.redis.test.fi;

import java.net.URI;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.DefaultJedisClientConfig;

/** A database the fault injector created: its id, first endpoint and credentials. */
public final class Database {

  private final long id;
  private final URI endpoint;
  private final String username;
  private final String password;
  private final boolean tls;

  private Database(long id, URI endpoint, String username, String password, boolean tls) {
    this.id = id;
    this.endpoint = endpoint;
    this.username = username;
    this.password = password;
    this.tls = tls;
  }

  /**
   * Parses a create_database action output: {@code {"bdb_id", "username", "password", "tls",
   * "endpoints": ["redis://host:port"]}}. The {@code default} user is folded into no user, as a
   * client config expects.
   */
  static Database parse(Map<String, Object> output) {
    String username = (String) output.get("username");
    return new Database(((Number) output.get("bdb_id")).longValue(),
        URI.create((String) ((List<?>) output.get("endpoints")).get(0)),
        username == null || "default".equals(username) ? null : username,
        (String) output.get("password"), Boolean.TRUE.equals(output.get("tls")));
  }

  public long id() {
    return id;
  }

  public URI endpoint() {
    return endpoint;
  }

  public String host() {
    return endpoint.getHost();
  }

  public int port() {
    return endpoint.getPort();
  }

  public boolean tls() {
    return tls;
  }

  /** A client config seeded with this database's credentials; the caller adds its own settings. */
  public DefaultJedisClientConfig.Builder clientConfig() {
    DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder().password(password);
    if (username != null) {
      config.user(username);
    }
    return config;
  }

  /** Never includes the credentials. */
  @Override
  public String toString() {
    return "db:" + id + " " + endpoint;
  }
}
