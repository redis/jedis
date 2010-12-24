package redis.clients.jedis;

public abstract class JedisTestConfigs {

    static public JedisConfig c1 = JedisConfig.newJedisConfig().password(
	    "foobared");
    static public JedisConfig c2 = JedisConfig.newJedisConfig()
	    .password("foobared").port(Protocol.DEFAULT_PORT + 1);

}
