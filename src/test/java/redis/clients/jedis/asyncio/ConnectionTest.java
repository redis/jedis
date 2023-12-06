package redis.clients.jedis.asyncio;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.asyncio.replies.StatusReply;
import redis.clients.jedis.asyncio.replies.Utf8StringReply;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

public class ConnectionTest extends JedisCommandsTestBase {

    @Test
    public void authAndSetByCommandArguments() throws Exception {
        final String strVal = "string-value";
        CommandObject<?> auth, set, get, ping;
        try (Connection connection = new Connection(hnp.getHost(), hnp.getPort())) {
            auth = connection.executeCommand(new CommandObject<>(
                    new CommandArguments(Protocol.Command.AUTH).add("foobared"),
                    new StatusReply())
            );
            set = connection.executeCommand(new CommandObject<>(
                    new CommandArguments(Protocol.Command.SET).key("foo").add(strVal),
                    new StatusReply())
            );
            get = connection.executeCommand(new CommandObject<>(
                    new CommandArguments(Protocol.Command.GET).key("foo"),
                    new Utf8StringReply())
            );
            ping = connection.executeCommand(new CommandObject<>( // just to get previous command
                    new CommandArguments(Protocol.Command.PING),
                    new StatusReply())
            ); // just to get previous command
        }

        assertEquals("OK", auth.getReply().get());
        assertEquals("OK", set.getReply().get());
        assertEquals(strVal, get.getReply().get());
        assertEquals("PONG", ping.getReply().get());

        Assert.assertEquals(strVal, jedis.get("foo"));
    }
}
