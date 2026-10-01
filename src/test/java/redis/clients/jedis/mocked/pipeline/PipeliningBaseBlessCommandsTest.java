package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.resps.ScanResult;

public class PipeliningBaseBlessCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testBlessSet() {
    when(commandObjects.blessSet("key", BlessFlag.NO_EVICT)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.blessSet("key", BlessFlag.NO_EVICT);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessSetBinary() {
    byte[] key = "key".getBytes();
    when(commandObjects.blessSet(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.blessSet(key, BlessFlag.NO_EVICT);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessClear() {
    when(commandObjects.blessClear("key", BlessFlag.NO_EVICT)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.blessClear("key", BlessFlag.NO_EVICT);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessClearBinary() {
    byte[] key = "key".getBytes();
    when(commandObjects.blessClear(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.blessClear(key, BlessFlag.NO_EVICT);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessGet() {
    when(commandObjects.blessGet("key")).thenReturn(listStringCommandObject);

    Response<java.util.List<String>> response = pipeliningBase.blessGet("key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessGetBinary() {
    byte[] key = "key".getBytes();
    when(commandObjects.blessGet(key)).thenReturn(listBytesCommandObject);

    Response<java.util.List<byte[]>> response = pipeliningBase.blessGet(key);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessScan() {
    when(commandObjects.blessScan("0", BlessFlag.NO_EVICT))
        .thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.blessScan("0", BlessFlag.NO_EVICT);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessScanWithCount() {
    when(commandObjects.blessScan("0", BlessFlag.NO_EVICT, 10))
        .thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.blessScan("0", BlessFlag.NO_EVICT, 10);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlessScanBinary() {
    byte[] cursor = "0".getBytes();
    when(commandObjects.blessScan(cursor, BlessFlag.NO_EVICT))
        .thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.blessScan(cursor, BlessFlag.NO_EVICT);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }
}
