package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;

public class PipeliningBaseCuckooFilterCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testCfAdd() {
    when(commandObjects.cfAdd("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfAdd("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfAddNx() {
    when(commandObjects.cfAddNx("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfAddNx("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfCount() {
    when(commandObjects.cfCount("myCuckooFilter", "item1")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.cfCount("myCuckooFilter", "item1");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfDel() {
    when(commandObjects.cfDel("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfDel("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfExists() {
    when(commandObjects.cfExists("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfExists("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInfo() {
    when(commandObjects.cfInfo("myCuckooFilter")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.cfInfo("myCuckooFilter");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsert() {
    when(commandObjects.cfInsert("myCuckooFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsert("myCuckooFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertWithParams() {
    CFInsertParams insertParams = new CFInsertParams().capacity(10000L).noCreate();

    when(commandObjects.cfInsert("myCuckooFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsert("myCuckooFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertNx() {
    when(commandObjects.cfInsertNx("myCuckooFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsertNx("myCuckooFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertNxWithParams() {
    CFInsertParams insertParams = new CFInsertParams().capacity(10000L).noCreate();

    when(commandObjects.cfInsertNx("myCuckooFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsertNx("myCuckooFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfLoadChunk() {
    byte[] data = { 1, 2, 3, 4 };

    when(commandObjects.cfLoadChunk("myCuckooFilter", 0L, data)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfLoadChunk("myCuckooFilter", 0L, data);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfMExists() {
    when(commandObjects.cfMExists("myCuckooFilter", "item1", "item2", "item3")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfMExists("myCuckooFilter", "item1", "item2", "item3");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfReserve() {
    when(commandObjects.cfReserve("myCuckooFilter", 10000L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfReserve("myCuckooFilter", 10000L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfReserveWithParams() {
    CFReserveParams reserveParams = new CFReserveParams().bucketSize(2).maxIterations(500).expansion(2);

    when(commandObjects.cfReserve("myCuckooFilter", 10000L, reserveParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfReserve("myCuckooFilter", 10000L, reserveParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfScanDump() {
    when(commandObjects.cfScanDump("myCuckooFilter", 0L)).thenReturn(entryLongBytesCommandObject);

    Response<Map.Entry<Long, byte[]>> response = pipeliningBase.cfScanDump("myCuckooFilter", 0L);

    assertThat(commands, contains(entryLongBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
