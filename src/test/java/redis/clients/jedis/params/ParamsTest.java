package redis.clients.jedis.params;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import redis.clients.jedis.args.ClientType;

public class ParamsTest {

  @Test
  public void toStringTest() {

    ClientKillParams clientKillParams = ClientKillParams.clientKillParams()
        .addr("127.0.0.1", 6379)
        .id("12".getBytes())
        .type(ClientType.NORMAL);

    String toStringResult = clientKillParams.toString();
    assertThat(toStringResult, CoreMatchers.containsString("ID, 12"));
    assertThat(toStringResult, CoreMatchers.containsString("TYPE, NORMAL"));
    assertThat(toStringResult, CoreMatchers.containsString("127.0.0.1:6379"));
  }

}
