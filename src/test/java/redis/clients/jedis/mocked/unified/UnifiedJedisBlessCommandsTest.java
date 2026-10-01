package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.args.BlessFlag;
import redis.clients.jedis.resps.ScanResult;

public class UnifiedJedisBlessCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testBlessSet() {
    String key = "bless-key";

    when(commandObjects.blessSet(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.blessSet(key, BlessFlag.NO_EVICT);

    assertThat(result, equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).blessSet(key, BlessFlag.NO_EVICT);
  }

  @Test
  public void testBlessSetBinary() {
    byte[] key = "bless-key".getBytes();

    when(commandObjects.blessSet(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.blessSet(key, BlessFlag.NO_EVICT);

    assertThat(result, equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).blessSet(key, BlessFlag.NO_EVICT);
  }

  @Test
  public void testBlessClear() {
    String key = "bless-key";

    when(commandObjects.blessClear(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(1L);

    long result = jedis.blessClear(key, BlessFlag.NO_EVICT);

    assertThat(result, equalTo(1L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).blessClear(key, BlessFlag.NO_EVICT);
  }

  @Test
  public void testBlessClearBinary() {
    byte[] key = "bless-key".getBytes();

    when(commandObjects.blessClear(key, BlessFlag.NO_EVICT)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(0L);

    long result = jedis.blessClear(key, BlessFlag.NO_EVICT);

    assertThat(result, equalTo(0L));
    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).blessClear(key, BlessFlag.NO_EVICT);
  }

  @Test
  public void testBlessGet() {
    String key = "bless-key";

    when(commandObjects.blessGet(key)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject))
        .thenReturn(Collections.singletonList("NO-EVICT"));

    assertThat(jedis.blessGet(key), contains("NO-EVICT"));
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).blessGet(key);
  }

  @Test
  public void testBlessGetBinary() {
    byte[] key = "bless-key".getBytes();

    when(commandObjects.blessGet(key)).thenReturn(listBytesCommandObject);
    when(commandExecutor.executeCommand(listBytesCommandObject))
        .thenReturn(Collections.emptyList());

    assertThat(jedis.blessGet(key), empty());
    verify(commandExecutor).executeCommand(listBytesCommandObject);
    verify(commandObjects).blessGet(key);
  }

  @Test
  public void testBlessScan() {
    ScanResult<String> scanResult = new ScanResult<>("0", Collections.singletonList("bless-key"));

    when(commandObjects.blessScan("0", BlessFlag.NO_EVICT))
        .thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(scanResult);

    ScanResult<String> result = jedis.blessScan("0", BlessFlag.NO_EVICT);

    assertThat(result.getCursor(), equalTo("0"));
    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).blessScan("0", BlessFlag.NO_EVICT);
  }

  @Test
  public void testBlessScanWithCount() {
    ScanResult<String> scanResult = new ScanResult<>("0", Collections.singletonList("bless-key"));

    when(commandObjects.blessScan("0", BlessFlag.NO_EVICT, 10))
        .thenReturn(scanResultStringCommandObject);
    when(commandExecutor.executeCommand(scanResultStringCommandObject)).thenReturn(scanResult);

    ScanResult<String> result = jedis.blessScan("0", BlessFlag.NO_EVICT, 10);

    assertThat(result.getCursor(), equalTo("0"));
    verify(commandExecutor).executeCommand(scanResultStringCommandObject);
    verify(commandObjects).blessScan("0", BlessFlag.NO_EVICT, 10);
  }

  @Test
  public void testBlessScanBinary() {
    byte[] cursor = "0".getBytes();
    ScanResult<byte[]> scanResult = new ScanResult<>("0",
        Collections.singletonList("bless-key".getBytes()));

    when(commandObjects.blessScan(cursor, BlessFlag.NO_EVICT))
        .thenReturn(scanResultBytesCommandObject);
    when(commandExecutor.executeCommand(scanResultBytesCommandObject)).thenReturn(scanResult);

    ScanResult<byte[]> result = jedis.blessScan(cursor, BlessFlag.NO_EVICT);

    assertThat(result.getCursor(), equalTo("0"));
    verify(commandExecutor).executeCommand(scanResultBytesCommandObject);
    verify(commandObjects).blessScan(cursor, BlessFlag.NO_EVICT);
  }
}
