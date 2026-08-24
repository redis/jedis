package com.redis.test.fi;

import java.util.Collections;
import java.util.Map;

/** One way to satisfy a {@link Trigger}: a ready-to-create dbconfig plus its preconditions. */
public final class Requirement {

  private final String config;
  private final String description;
  private final int minNodes;
  private final Map<String, Object> dbConfig;

  private Requirement(String config, String description, int minNodes,
      Map<String, Object> dbConfig) {
    this.config = config;
    this.description = description;
    this.minNodes = minNodes;
    this.dbConfig = Collections.unmodifiableMap(dbConfig);
  }

  @SuppressWarnings("unchecked")
  static Requirement parse(Map<String, Object> requirement) {
    Map<String, Object> cluster = (Map<String, Object>) requirement.get("cluster");
    int minNodes = cluster == null ? 0 : ((Number) cluster.get("min_nodes")).intValue();
    return new Requirement((String) requirement.get("config"),
        (String) requirement.get("description"), minNodes,
        (Map<String, Object>) requirement.get("dbconfig"));
  }

  /** Catalog entry name: single, single_tls, scaled_single, mtls. */
  public String config() {
    return config;
  }

  public String description() {
    return description;
  }

  public int minNodes() {
    return minNodes;
  }

  /** The dbconfig to pass verbatim to {@link FaultInjectorClient#createDatabase}. */
  public Map<String, Object> dbConfig() {
    return dbConfig;
  }

  @Override
  public String toString() {
    return "Requirement[" + config + "]";
  }
}
