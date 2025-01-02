package redis.clients.jedis;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

@RunWith(Parameterized.class)
public class JedisBinaryStreamTest extends JedisCommandsTestBase {
    public JedisBinaryStreamTest(RedisProtocol protocol) {
        super(protocol);
    }
}
