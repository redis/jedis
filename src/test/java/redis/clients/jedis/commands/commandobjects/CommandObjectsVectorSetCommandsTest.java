package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.VAddParams;
import redis.clients.jedis.params.VSimParams;
import redis.clients.jedis.resps.RawVector;
import redis.clients.jedis.resps.VSimResult;

/**
 * Tests related to <a href="https://redis.io/docs/latest/commands/?group=vector-set">Vector Set</a> commands.
 */
public class CommandObjectsVectorSetCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsVectorSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testVaddBasic() {
    String key = "test:vector:set";
    float[] vector = {1.0f, 2.0f, 3.0f};
    String element = "element1";

    CommandObject<Boolean> cmd = commandObjects.vadd(key, vector, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVaddWithParams() {
    String key = "test:vector:set";
    float[] vector = {1.0f, 2.0f, 3.0f};
    String element = "element1";
    VAddParams params = new VAddParams().cas().q8();

    CommandObject<Boolean> cmd = commandObjects.vadd(key, vector, element, params);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVaddFP32() {
    String key = "test:vector:set";
    byte[] vectorBlob = new byte[]{0x00, 0x00, (byte)0x80, 0x3f}; // 1.0f in FP32 format
    String element = "element1";

    CommandObject<Boolean> cmd = commandObjects.vaddFP32(key, vectorBlob, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVaddFP32WithParams() {
    String key = "test:vector:set";
    byte[] vectorBlob = new byte[]{0x00, 0x00, (byte)0x80, 0x3f}; // 1.0f in FP32 format
    String element = "element1";
    VAddParams params = new VAddParams().noQuant();

    CommandObject<Boolean> cmd = commandObjects.vaddFP32(key, vectorBlob, element, params);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimBasic() {
    String key = "test:vector:set";
    float[] vector = {1.0f, 2.0f, 3.0f};

    CommandObject<List<String>> cmd = commandObjects.vsim(key, vector);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimWithParams() {
    String key = "test:vector:set";
    float[] vector = {1.0f, 2.0f, 3.0f};
    VSimParams params = new VSimParams().withScores().count(10);

    CommandObject<VSimResult> cmd = commandObjects.vsim(key, vector, params);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimByElement() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<List<String>> cmd = commandObjects.vsimByElement(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimByElementWithParams() {
    String key = "test:vector:set";
    String element = "element1";
    VSimParams params = new VSimParams().withScores().ef(100);

    CommandObject<VSimResult> cmd = commandObjects.vsimByElement(key, element, params);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimFP32() {
    String key = "test:vector:set";
    byte[] vectorBlob = new byte[]{0x00, 0x00, (byte)0x80, 0x3f}; // 1.0f in FP32 format

    CommandObject<List<String>> cmd = commandObjects.vsimFP32(key, vectorBlob);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimFP32WithParams() {
    String key = "test:vector:set";
    byte[] vectorBlob = new byte[]{0x00, 0x00, (byte)0x80, 0x3f}; // 1.0f in FP32 format
    VSimParams params = new VSimParams().withScores().truth();

    CommandObject<VSimResult> cmd = commandObjects.vsimFP32(key, vectorBlob, params);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVdim() {
    String key = "test:vector:set";

    CommandObject<Long> cmd = commandObjects.vdim(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVcard() {
    String key = "test:vector:set";

    CommandObject<Long> cmd = commandObjects.vcard(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVemb() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<List<Double>> cmd = commandObjects.vemb(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVembRaw() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<RawVector> cmd = commandObjects.vembRaw(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVrem() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<Boolean> cmd = commandObjects.vrem(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVlinks() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<List<String>> cmd = commandObjects.vlinks(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVlinksWithScores() {
    String key = "test:vector:set";
    String element = "element1";

    CommandObject<Map<String, Double>> cmd = commandObjects.vlinksWithScores(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVrandmember() {
    String key = "test:vector:set";

    CommandObject<String> cmd = commandObjects.vrandmember(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVrandmemberWithCount() {
    String key = "test:vector:set";
    int count = 3;

    CommandObject<List<String>> cmd = commandObjects.vrandmember(key, count);
    assertThat(cmd, notNullValue());
  }

  // Binary key tests
  @Test
  public void testVaddBinary() {
    byte[] key = "test:vector:set".getBytes();
    float[] vector = {1.0f, 2.0f, 3.0f};
    byte[] element = "element1".getBytes();

    CommandObject<Boolean> cmd = commandObjects.vadd(key, vector, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVsimBinary() {
    byte[] key = "test:vector:set".getBytes();
    float[] vector = {1.0f, 2.0f, 3.0f};

    CommandObject<List<byte[]>> cmd = commandObjects.vsim(key, vector);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVdimBinary() {
    byte[] key = "test:vector:set".getBytes();

    CommandObject<Long> cmd = commandObjects.vdim(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVcardBinary() {
    byte[] key = "test:vector:set".getBytes();

    CommandObject<Long> cmd = commandObjects.vcard(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVembBinary() {
    byte[] key = "test:vector:set".getBytes();
    byte[] element = "element1".getBytes();

    CommandObject<List<Double>> cmd = commandObjects.vemb(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVembRawBinary() {
    byte[] key = "test:vector:set".getBytes();
    byte[] element = "element1".getBytes();

    CommandObject<RawVector> cmd = commandObjects.vembRaw(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVremBinary() {
    byte[] key = "test:vector:set".getBytes();
    byte[] element = "element1".getBytes();

    CommandObject<Boolean> cmd = commandObjects.vrem(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVlinksBinary() {
    byte[] key = "test:vector:set".getBytes();
    byte[] element = "element1".getBytes();

    CommandObject<List<byte[]>> cmd = commandObjects.vlinks(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVlinksWithScoresBinary() {
    byte[] key = "test:vector:set".getBytes();
    byte[] element = "element1".getBytes();

    CommandObject<Map<byte[], Double>> cmd = commandObjects.vlinksWithScores(key, element);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVrandmemberBinary() {
    byte[] key = "test:vector:set".getBytes();

    CommandObject<byte[]> cmd = commandObjects.vrandmember(key);
    assertThat(cmd, notNullValue());
  }

  @Test
  public void testVrandmemberWithCountBinary() {
    byte[] key = "test:vector:set".getBytes();
    int count = 3;

    CommandObject<List<byte[]>> cmd = commandObjects.vrandmember(key, count);
    assertThat(cmd, notNullValue());
  }

  // Parameter tests
  @Test
  public void testVAddParamsBuilder() {
    VAddParams params = new VAddParams()
        .cas()
        .q8()
        .ef(200)
        .setAttr("{\"category\": \"test\"}")
        .m(16);

    assertThat(params, notNullValue());
  }

  @Test
  public void testVSimParamsBuilder() {
    VSimParams params = new VSimParams()
        .withScores()
        .count(50)
        .ef(100)
        .filter("@category == 'test'")
        .filterEf(500)
        .truth()
        .noThread();

    assertThat(params, notNullValue());
    assertTrue(params.isWithScores());
  }

  @Test
  public void testVSimResultBasic() {
    List<String> elements = Arrays.asList("element1", "element2", "element3");
    VSimResult result = new VSimResult(elements);

    assertThat(result.getElements(), equalTo(elements));
    assertThat(result.size(), equalTo(3));
    assertFalse(result.hasScores());
    assertNull(result.getScores());
    assertFalse(result.isEmpty());
  }

  @Test
  public void testVSimResultWithScores() {
    List<String> elements = Arrays.asList("element1", "element2");
    Map<String, Double> scores = Stream.of(
        new SimpleEntry<>("element1", 0.95),
        new SimpleEntry<>("element2",  0.87)
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    VSimResult result = new VSimResult(elements, scores);

    assertThat(result.getElements(), equalTo(elements));
    assertThat(result.size(), equalTo(2));
    assertTrue(result.hasScores());
    assertThat(result.getScores(), equalTo(scores));
    assertThat(result.getScore("element1"), equalTo(0.95));
    assertThat(result.getScore("element2"), equalTo(0.87));
    assertNull(result.getScore("nonexistent"));
    assertFalse(result.isEmpty());
  }

  @Test
  public void testVEmbResultRaw() {
    String quantizationType = "fp32";
    byte[] rawData = new byte[]{0x00, 0x00, (byte)0x80, 0x3f}; // 1.0f in FP32 format
    Double norm = 2.5;
    Double quantizationRange = null; // not q8

    RawVector result = new RawVector(quantizationType, rawData, norm, quantizationRange);

    assertThat(result.getQuantizationType(), equalTo(quantizationType));
    assertThat(result.getRawData(), equalTo(rawData));
    assertThat(result.getNorm(), equalTo(norm));
    assertNull(result.getQuantizationRange());
  }

  @Test
  public void testVEmbResultRawWithQuantizationRange() {
    String quantizationType = "q8";
    byte[] rawData = new byte[]{0x01, 0x02, 0x03};
    Double norm = 1.8;
    Double quantizationRange = 127.0;

    RawVector result = new RawVector(quantizationType, rawData, norm, quantizationRange);

    assertThat(result.getQuantizationType(), equalTo(quantizationType));
    assertThat(result.getRawData(), equalTo(rawData));
    assertThat(result.getNorm(), equalTo(norm));
    assertThat(result.getQuantizationRange(), equalTo(quantizationRange));
  }
}
