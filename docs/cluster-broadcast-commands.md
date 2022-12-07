# Broadcast commands to all Redis Cluster nodes

When using [Open Source Redis Cluster API](https://redis.io/docs/reference/cluster-spec/), client is responsible for applying some commands against all primary nodes. To simplify this task, Jedis provides an easy way to broadcast commands and handle occurring errors during the broadcast process.

For example, if we want to update the configuration of the Redis Cluster, we need to broadcast the command [CONFIG SET](https://redis.io/commands/config-set/) to all nodes:

```java
package org.example;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Map;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) {
        
        var clusterNode = new HostAndPort("127.0.0.1", 7000);
        JedisCluster client = new JedisCluster(clusterNode);
        JedisBroadcast broadcast = client.broadcast();

        Map<?, Supplier<String>> replies = broadcast.configSet("maxmemory", "100mb");

        for (var entry : replies.entrySet()) {
            Object node = entry.getKey();

            try {
                String reply = entry.getValue().get();
                // process reply ...
            } catch (JedisDataException jde) {
                UnifiedJedis innerClient = new UnifiedJedis(new Connection((HostAndPort) node));
                // retry command or handle the error
            }
        }
        
    }
}
```

Full list of all commands that can be automatically broadcasted you can find [here](https://github.com/redis/jedis/blob/master/src/main/java/redis/clients/jedis/JedisBroadcast.java).