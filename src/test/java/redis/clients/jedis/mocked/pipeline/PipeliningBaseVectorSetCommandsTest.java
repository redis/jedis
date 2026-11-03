package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.VSimParams;
import redis.clients.jedis.resps.VSimScoreAttribs;

public class PipeliningBaseVectorSetCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testVsimWithScoresAndAttribs() {
    float[] vector = { 1.0f, 0.0f };
    when(commandObjects.vsimWithScoresAndAttribs("key", vector))
        .thenReturn(mapStringVSimScoreAttribsCommandObject);

    Response<Map<String, VSimScoreAttribs>> response = pipeliningBase
        .vsimWithScoresAndAttribs("key", vector);

    assertThat(commands, contains(mapStringVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimWithScoresAndAttribsWithParams() {
    float[] vector = { 1.0f, 0.0f };
    VSimParams params = new VSimParams();
    when(commandObjects.vsimWithScoresAndAttribs("key", vector, params))
        .thenReturn(mapStringVSimScoreAttribsCommandObject);

    Response<Map<String, VSimScoreAttribs>> response = pipeliningBase
        .vsimWithScoresAndAttribs("key", vector, params);

    assertThat(commands, contains(mapStringVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimWithScoresAndAttribsBinary() {
    byte[] key = { 'k' };
    float[] vector = { 1.0f, 0.0f };
    when(commandObjects.vsimWithScoresAndAttribs(key, vector))
        .thenReturn(mapBytesVSimScoreAttribsCommandObject);

    Response<Map<byte[], VSimScoreAttribs>> response = pipeliningBase.vsimWithScoresAndAttribs(key,
      vector);

    assertThat(commands, contains(mapBytesVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimWithScoresAndAttribsBinaryWithParams() {
    byte[] key = { 'k' };
    float[] vector = { 1.0f, 0.0f };
    VSimParams params = new VSimParams();
    when(commandObjects.vsimWithScoresAndAttribs(key, vector, params))
        .thenReturn(mapBytesVSimScoreAttribsCommandObject);

    Response<Map<byte[], VSimScoreAttribs>> response = pipeliningBase.vsimWithScoresAndAttribs(key,
      vector, params);

    assertThat(commands, contains(mapBytesVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimByElementWithScoresAndAttribs() {
    when(commandObjects.vsimByElementWithScoresAndAttribs("key", "element"))
        .thenReturn(mapStringVSimScoreAttribsCommandObject);

    Response<Map<String, VSimScoreAttribs>> response = pipeliningBase
        .vsimByElementWithScoresAndAttribs("key", "element");

    assertThat(commands, contains(mapStringVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimByElementWithScoresAndAttribsWithParams() {
    VSimParams params = new VSimParams();
    when(commandObjects.vsimByElementWithScoresAndAttribs("key", "element", params))
        .thenReturn(mapStringVSimScoreAttribsCommandObject);

    Response<Map<String, VSimScoreAttribs>> response = pipeliningBase
        .vsimByElementWithScoresAndAttribs("key", "element", params);

    assertThat(commands, contains(mapStringVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimByElementWithScoresAndAttribsBinary() {
    byte[] key = { 'k' };
    byte[] element = { 'e' };
    when(commandObjects.vsimByElementWithScoresAndAttribs(key, element))
        .thenReturn(mapBytesVSimScoreAttribsCommandObject);

    Response<Map<byte[], VSimScoreAttribs>> response = pipeliningBase
        .vsimByElementWithScoresAndAttribs(key, element);

    assertThat(commands, contains(mapBytesVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testVsimByElementWithScoresAndAttribsBinaryWithParams() {
    byte[] key = { 'k' };
    byte[] element = { 'e' };
    VSimParams params = new VSimParams();
    when(commandObjects.vsimByElementWithScoresAndAttribs(key, element, params))
        .thenReturn(mapBytesVSimScoreAttribsCommandObject);

    Response<Map<byte[], VSimScoreAttribs>> response = pipeliningBase
        .vsimByElementWithScoresAndAttribs(key, element, params);

    assertThat(commands, contains(mapBytesVSimScoreAttribsCommandObject));
    assertThat(response, is(predefinedResponse));
  }
}
