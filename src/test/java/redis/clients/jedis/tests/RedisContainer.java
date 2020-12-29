package redis.clients.jedis.tests;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainer extends GenericContainer<RedisContainer> {
    public RedisContainer() {
      super(DockerImageName.parse("redis:6.0.9"));
    }

    public String getRedisUri() {
        return "redis://" + this.getHost() + ":" + this.getMappedPort(6379);
    }
}
