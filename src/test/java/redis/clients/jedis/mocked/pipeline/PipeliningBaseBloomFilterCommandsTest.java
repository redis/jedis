package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;

public class PipeliningBaseBloomFilterCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testBfAdd() {
    when(commandObjects.bfAdd("myBloomFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.bfAdd("myBloomFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfCard() {
    when(commandObjects.bfCard("myBloomFilter")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bfCard("myBloomFilter");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfExists() {
    when(commandObjects.bfExists("myBloomFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.bfExists("myBloomFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInfo() {
    when(commandObjects.bfInfo("myBloomFilter")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.bfInfo("myBloomFilter");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInsert() {
    when(commandObjects.bfInsert("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfInsert("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInsertWithParams() {
    BFInsertParams insertParams = new BFInsertParams().capacity(10000L).error(0.01);

    when(commandObjects.bfInsert("myBloomFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfInsert("myBloomFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfLoadChunk() {
    byte[] data = { 1, 2, 3, 4 };

    when(commandObjects.bfLoadChunk("myBloomFilter", 0L, data)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfLoadChunk("myBloomFilter", 0L, data);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfMAdd() {
    when(commandObjects.bfMAdd("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfMAdd("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfMExists() {
    when(commandObjects.bfMExists("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfMExists("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfReserve() {
    double errorRate = 0.01;
    long capacity = 10000L;

    when(commandObjects.bfReserve("myBloomFilter", errorRate, capacity)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfReserve("myBloomFilter", errorRate, capacity);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfReserveWithParams() {
    double errorRate = 0.01;
    long capacity = 10000L;

    BFReserveParams reserveParams = new BFReserveParams().expansion(2);
    when(commandObjects.bfReserve("myBloomFilter", errorRate, capacity, reserveParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfReserve("myBloomFilter", errorRate, capacity, reserveParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfScanDump() {
    when(commandObjects.bfScanDump("myBloomFilter", 0L)).thenReturn(entryLongBytesCommandObject);

    Response<Map.Entry<Long, byte[]>> response = pipeliningBase.bfScanDump("myBloomFilter", 0L);

    assertThat(commands, contains(entryLongBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
