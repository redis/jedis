# Broadcast commands to all Redis Cluster nodes

When using [Open Source Redis Cluster API](https://redis.io/docs/reference/cluster-spec/), client is responsible for 
applying some commands against all primary nodes. To simplify this task, Jedis provides an easy way to broadcast 
commands.

For example, if we want to update the configuration of the Redis Cluster, we need to broadcast the command 
[CONFIG SET](https://redis.io/commands/config-set/) to all nodes:

```java
import java.util.Map;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

public class Main {
    public static void main(String[] args) {

        HostAndPort clusterNode = new HostAndPort("127.0.0.1", 7000);
        JedisCluster client = new JedisCluster(clusterNode);

        String reply = client.configSet("maxmemory", "100mb"); // reply is "OK"
    }
}
```
