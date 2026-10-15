package redis.clients.jedis.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.ClusterCommandObjects;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;

/**
 * Routing of the commands that {@link RedisClusterClient} sends through
 * {@code executeCommandToReplica}, against a cluster of three primaries that each have one replica.
 */
public class ClusterReplicaRoutingIT {

  private static final RedisProtocol PROTOCOL = RedisProtocol.RESP3;

  private static final String KEY = "replica-routing-it";

  private static final ClusterCommandObjects commandObjects = new ClusterCommandObjects(PROTOCOL);

  private static EndpointConfig endpoint;
  private static Set<HostAndPort> nodes;

  @BeforeAll
  public static void setUpClass() {
    endpoint = Endpoints.getRedisEndpoint("cluster-stable");
    nodes = new HashSet<>(endpoint.getHostsAndPorts());
  }

  @Test
  public void keylessCommandRunsOnAReplicaWhenTheTopologyRecordsReplicas() {
    try (RedisClusterClient client = clusterClient(true)) {
      assertThat(client.executeCommandToReplica(commandObjects.info("replication")),
        containsString("role:slave"));
    }
  }

  @Test
  public void keylessCommandRunsOnAPrimaryWhenNoReplicaIsRecorded() {
    try (RedisClusterClient client = clusterClient(false)) {
      assertThat(client.executeCommandToReplica(commandObjects.info("replication")),
        containsString("role:master"));
    }
  }

  @Test
  public void keyedCommandReadsItsValueWhenNoReplicaIsRecorded() {
    try (RedisClusterClient client = clusterClient(false)) {
      assertEquals("OK", client.set(KEY, "value"));

      assertEquals("value", client.executeCommandToReplica(commandObjects.get(KEY)));

      client.del(KEY);
    }
  }

  private static RedisClusterClient clusterClient(boolean readOnlyForRedisClusterReplicas) {
    DefaultJedisClientConfig.Builder config = endpoint.getClientConfigBuilder().protocol(PROTOCOL);
    if (readOnlyForRedisClusterReplicas) {
      config.readOnlyForRedisClusterReplicas();
    }

    return RedisClusterClient.builder().nodes(nodes).clientConfig(config.build()).build();
  }
}
