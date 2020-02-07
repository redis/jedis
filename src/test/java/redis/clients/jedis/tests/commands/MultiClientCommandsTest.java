package redis.clients.jedis.tests.commands;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import static org.junit.Assert.assertTrue;

/**
 * Tests to test multiple client
 */
public class MultiClientCommandsTest extends JedisCommandTestBase {
    private final String clientName = "fancy_jedis_name";
    private final String clientName2 = "fancy_jedis_another_name";


    protected Jedis client;
    private Jedis client2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = new Jedis(hnp.getHost(), hnp.getPort(), 500);
        client.auth("foobared");
        client.clientSetname(clientName);

        client2 = new Jedis(hnp.getHost(), hnp.getPort(), 500);
        client2.auth("foobared");
        client2.clientSetname(clientName2);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        client.close();
        client2.close();
        super.tearDown();
    }

    /**
     * Test - If the ID of a connection is greater than the ID of another connection,
     * it is guaranteed that the second connection was established with the server at a later time.
     */
    @Test
    public void clientIdMultipleConnection() {
        long clientId1 = client.clientId();
        long clientId2 = client2.clientId();

        assertTrue(clientId2 > clientId1);
    }

    /**
     * Test - It is never repeated, so if CLIENT ID returns the same number,
     * the caller can be sure that the underlying client did not disconnect and reconnect the connection,
     * but it is still the same connection.
     */
    public void clientReconnectTest() {
        long clientId1 = client.clientId();
        client.close();

        client = new Jedis(hnp.getHost(), hnp.getPort(), 500);
        client.auth("foobared");
        client.clientSetname(clientName);

        long clientId2 = client.clientId();

        assertTrue(clientId2 != clientId1);
        assertTrue(clientId2 > clientId1);
    }
}
