package redis.clients.jedis.json;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.*;
import static org.hamcrest.Matchers.not;

public class JsonSetParamsTest {

  @Test
  public void testNxAndXxMutuallyExclusive() {
    // NX and XX should be mutually exclusive
    JsonSetParams params1 = new JsonSetParams().nx().xx();
    CommandArguments args1 = new CommandArguments(Protocol.Command.SET);
    params1.addParams(args1);
    assertThat(args1, containsArguments(Keyword.XX));
    assertThat(args1, not(containsArguments(Keyword.NX)));

    JsonSetParams params2 = new JsonSetParams().xx().nx();
    CommandArguments args2 = new CommandArguments(Protocol.Command.SET);
    params2.addParams(args2);

    assertThat(args2, containsArguments(Keyword.NX));
    assertThat(args2, not(containsArguments(Keyword.XX)));
  }

  @Test
  public void testFphaTypes() {
    // Test that each fpha type is correctly added
    assertFphaType(new JsonSetParams().fp16(), JsonSetParams.FphaType.FP16);
    assertFphaType(new JsonSetParams().bf16(), JsonSetParams.FphaType.BF16);
    assertFphaType(new JsonSetParams().fp32(), JsonSetParams.FphaType.FP32);
    assertFphaType(new JsonSetParams().fp64(), JsonSetParams.FphaType.FP64);
  }

  @Test
  public void testFphaOverride() {
    // Setting another fpha type should override the previous one (last wins)
    JsonSetParams params = new JsonSetParams().fp16().fp32();
    CommandArguments args = new CommandArguments(Protocol.Command.SET);
    params.addParams(args);

    assertThat(args, containsArguments(JsonSetParams.FphaType.FP32));
    assertThat(args, not(containsArguments(JsonSetParams.FphaType.FP16)));
  }

  @Test
  public void testCombinedParams() {
    // Test combining NX/XX with fpha types
    JsonSetParams params = new JsonSetParams().nx().fp16();
    CommandArguments args = new CommandArguments(Protocol.Command.SET);
    params.addParams(args);

    assertThat(args, containsArguments(Keyword.NX));
    assertThat(args, containsArguments(JsonSetParams.FphaType.FP16));
  }

  @Test
  public void testEmptyParams() {
    JsonSetParams params = new JsonSetParams();
    CommandArguments args = new CommandArguments(Protocol.Command.SET);
    params.addParams(args);

    // Should not contain any optional parameters
    assertThat(args, not(containsArguments(Keyword.NX)));
    assertThat(args, not(containsArguments(Keyword.XX)));
    assertThat(args, not(containsArguments(JsonSetParams.FphaType.FP16)));
    assertThat(args, not(containsArguments(JsonSetParams.FphaType.BF16)));
    assertThat(args, not(containsArguments(JsonSetParams.FphaType.FP32)));
    assertThat(args, not(containsArguments(JsonSetParams.FphaType.FP64)));
  }

  /**
   * Helper method to assert a specific fpha type
   */
  private void assertFphaType(JsonSetParams params, JsonSetParams.FphaType expectedType) {
    CommandArguments args = new CommandArguments(Protocol.Command.SET);
    params.addParams(args);
    assertThat(args, hasArgumentCount(3));
    assertThat(args, containsArguments(Protocol.Command.SET, Keyword.FPHA, expectedType));
  }

}
