package redis.clients.jedis.tests.params;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.params.ClientKillParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;

import static org.junit.Assert.assertEquals;

public class ParamsTest {

    @Test
    public void toStringTest() {

        ClientKillParams clientKillParams = ClientKillParams.clientKillParams()
                .addr("127.0.0.1", 6379)
                .id( "12".getBytes() )
                .type(ClientKillParams.Type.NORMAL)
                ;

        String toStringResult = clientKillParams.toString();
        Assert.assertThat(toStringResult, CoreMatchers.containsString("ID, 12"));
        Assert.assertThat(toStringResult, CoreMatchers.containsString("TYPE, NORMAL"));
        Assert.assertThat(toStringResult, CoreMatchers.containsString("127.0.0.1:6379"));

    }

}
