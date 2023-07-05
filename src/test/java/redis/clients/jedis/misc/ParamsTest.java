//package redis.clients.jedis.misc;
//
//import static org.hamcrest.CoreMatchers.containsString;
//import static org.hamcrest.MatcherAssert.assertThat;
//
//import org.junit.Test;
//import redis.clients.jedis.args.ClientType;
//import redis.clients.jedis.params.ClientKillParams;
//
//public class ParamsTest {
//
//  @Test
//  public void toStringTest() {
//
//    ClientKillParams clientKillParams = ClientKillParams.clientKillParams()
//        .addr("127.0.0.1", 6379)
//        .id("12".getBytes())
//        .type(ClientType.NORMAL);
//
//    String toStringResult = clientKillParams.toString();
//    assertThat(toStringResult, containsString("ID, 12"));
//    assertThat(toStringResult, containsString("TYPE, NORMAL"));
//    assertThat(toStringResult, containsString("127.0.0.1:6379"));
//  }
//
//}
