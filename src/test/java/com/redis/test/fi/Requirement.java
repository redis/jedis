package com.redis.test.fi;

import java.util.Collections;
import java.util.Map;

/**
 * One database configuration a trigger can run under. Standalone requirements are named by the FI
 * ({@code single}, {@code single_tls}, ...); slot-migrate requirements carry no name and are
 * identified by their OSS-cluster endpoint type plus TLS / ASM flags, from which {@link #config()}
 * is derived so both families select variants the same way.
 */
public final class Requirement {

  private final String config;
  private final String description;
  private final int minNodes;
  private final Map<String, Object> dbConfig;
  private final String ipType;
  private final String endpointType;
  private final boolean tls;
  private final boolean asm;
  private final int index;

  private Requirement(String config, String description, int minNodes, Map<String, Object> dbConfig,
      String ipType, String endpointType, boolean tls, boolean asm, int index) {
    this.config = config;
    this.description = description;
    this.minNodes = minNodes;
    this.dbConfig = Collections.unmodifiableMap(dbConfig);
    this.ipType = ipType;
    this.endpointType = endpointType;
    this.tls = tls;
    this.asm = asm;
    this.index = index;
  }

  @SuppressWarnings("unchecked")
  static Requirement parse(Map<String, Object> requirement, int index) {
    Map<String, Object> cluster = (Map<String, Object>) requirement.get("cluster");
    int minNodes = cluster == null ? 0 : ((Number) cluster.get("min_nodes")).intValue();
    Map<String, Object> ossClusterApi = (Map<String, Object>) requirement.get("oss_cluster_api");
    String ipType = ossClusterApi == null ? null : (String) ossClusterApi.get("ip_type");
    String endpointType = ossClusterApi == null ? null
        : (String) ossClusterApi.get("endpoint_type");
    boolean tls = Boolean.TRUE.equals(requirement.get("tls"));
    boolean asm = Boolean.TRUE.equals(requirement.get("asm"));

    String config = (String) requirement.get("config");
    if (config == null && ipType != null) {
      config = ipType + "/" + endpointType + (tls ? "+tls" : "") + (asm ? "+asm" : "");
    }
    return new Requirement(config, (String) requirement.get("description"), minNodes,
        (Map<String, Object>) requirement.get("dbconfig"), ipType, endpointType, tls, asm, index);
  }

  /**
   * Catalog entry name: {@code single}, {@code single_tls}, {@code scaled_single}, {@code mtls} for
   * standalone; {@code external/ip}, {@code external/hostname}, {@code external/hostname+tls},
   * {@code external/ip+asm}, ... for slot-migrate.
   */
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

  /** OSS-cluster {@code ip_type} ({@code external} / {@code internal}); null for standalone. */
  public String ipType() {
    return ipType;
  }

  /** OSS-cluster {@code endpoint_type} ({@code ip} / {@code hostname}); null for standalone. */
  public String endpointType() {
    return endpointType;
  }

  public boolean tls() {
    return tls;
  }

  /** Whether the database needs the ASM setup / teardown lifecycle instead of create / delete. */
  public boolean asm() {
    return asm;
  }

  /** Position in the trigger's requirement list; the FI's {@code variant_index}. */
  public int index() {
    return index;
  }

  @Override
  public String toString() {
    return "Requirement[" + config + "]";
  }
}
