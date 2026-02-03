package redis.clients.jedis;

import org.opentest4j.TestAbortedException;
import redis.clients.jedis.util.TestEnvUtil;

import java.util.HashMap;

public final class Endpoints {

  // Cluster endpoints
  public static final String CLUSTER = "cluster";
  public static final String CLUSTER_ENTRAID_ACL = "cluster-entraid-acl";
  public static final String CLUSTER_STABLE = "cluster-stable";
  public static final String CLUSTER_UNBOUND = "cluster-unbound";
  public static final String CLUSTER_UNBOUND_TLS = "cluster-unbound-tls";

  // Modules endpoint
  public static final String MODULES_DOCKER = "modules-docker";

  // Redis Enterprise endpoints
  public static final String RE_ACTIVE_ACTIVE = "re-active-active";
  public static final String RE_SINGLE_SHARD_OSS_CLUSTER = "re-single-shard-oss-cluster";
  public static final String RE_STANDALONE = "re-standalone";

  // Redis failover endpoints
  public static final String REDIS_FAILOVER_1 = "redis-failover-1";
  public static final String REDIS_FAILOVER_2 = "redis-failover-2";

  // Sentinel endpoints
  public static final String SENTINEL_FAILOVER = "sentinel-failover";
  public static final String SENTINEL_STANDALONE0 = "sentinel-standalone0";
  public static final String SENTINEL_STANDALONE2_1 = "sentinel-standalone2-1";
  public static final String SENTINEL_STANDALONE2_3 = "sentinel-standalone2-3";

  // Standalone endpoints
  public static final String STANDALONE0 = "standalone0";
  public static final String STANDALONE0_ACL = "standalone0-acl";
  public static final String STANDALONE0_ACL_TLS = "standalone0-acl-tls";
  public static final String STANDALONE0_TLS = "standalone0-tls";
  public static final String STANDALONE1 = "standalone1";
  public static final String STANDALONE2_PRIMARY = "standalone2-primary";
  public static final String STANDALONE3_REPLICA_OF_STANDALONE2 = "standalone3-replica-of-standalone2";
  public static final String STANDALONE4_REPLICA_OF_STANDALONE1 = "standalone4-replica-of-standalone1";
  public static final String STANDALONE7_WITH_LFU_POLICY = "standalone7-with-lfu-policy";
  public static final String STANDALONE9_FAILOVER = "standalone9-failover";
  public static final String STANDALONE10_REPLICA_OF_STANDALONE9 = "standalone10-replica-of-standalone9";
  public static final String STANDALONE_ENTRAID_ACL = "standalone-entraid-acl";

  private static final HashMap<String, EndpointConfig> endpointConfigs;

  static {
    try {
      endpointConfigs = EndpointConfig.loadFromJSON(TestEnvUtil.getEndpointsConfigPath());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static EndpointConfig getRedisEndpoint(String endpointName) {
    if (!endpointConfigs.containsKey(endpointName)) {
      throw new TestAbortedException("Unavailable Redis endpoint: " + endpointName);
    }

    return endpointConfigs.get(endpointName);
  }

  private Endpoints() {
    throw new InstantiationError("Must not instantiate this class");
  }
}
