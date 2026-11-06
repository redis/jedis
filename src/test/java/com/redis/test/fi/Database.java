package com.redis.test.fi;

import java.net.URI;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.DefaultJedisClientConfig;

/** A database the fault injector created for a test: id, first endpoint, credentials, TLS. */
public final class Database {

  private final long id;
  private final URI endpoint;
  private final String username;
  private final String password;
  private final boolean tls;
  private final String setupId;

  private Database(long id, URI endpoint, String username, String password, boolean tls,
      String setupId) {
    this.id = id;
    this.endpoint = endpoint;
    this.username = username;
    this.password = password;
    this.tls = tls;
    this.setupId = setupId;
  }

  /**
   * Parses a create_database action output or a {@code /slot-migrate/setup} response:
   * {@code {"bdb_id", "username", "password", "tls", "endpoints": ["redis://host:port"],
   * ["setup_id"]}}. The {@code default} user is folded into no user, as a client config expects.
   */
  static Database parse(Map<String, Object> output) {
    String username = (String) output.get("username");
    Object setupId = output.get("setup_id");
    return new Database(((Number) output.get("bdb_id")).longValue(),
        URI.create((String) ((List<?>) output.get("endpoints")).get(0)),
        username == null || "default".equals(username) ? null : username,
        (String) output.get("password"), Boolean.TRUE.equals(output.get("tls")),
        setupId == null ? null : setupId.toString());
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

  /**
   * The ASM setup this database belongs to, or null when it was created with create_database. A
   * setup database is released with {@code /slot-migrate/teardown}, not delete_database.
   */
  public String setupId() {
    return setupId;
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
    return "db:" + id + " " + endpoint + (setupId == null ? "" : " setup:" + setupId);
  }
}
