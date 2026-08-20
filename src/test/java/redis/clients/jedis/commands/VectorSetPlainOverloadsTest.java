package redis.clients.jedis.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.RedisProtocol;

/**
 * #4688 PR A: missing plain vector-set overloads must encode the same command as the params
 * overload with {@code null} params.
 */
public class VectorSetPlainOverloadsTest {

  private final CommandObjects commands = new CommandObjects(RedisProtocol.RESP2);

  @Test
  public void vaddWithReduceDimDelegatesWithoutParams() {
    float[] vector = { 1.0f, 2.0f, 3.0f, 4.0f };
    assertEquals(commands.vadd("vs", vector, "el", 2, null), commands.vadd("vs", vector, "el", 2));
  }

  @Test
  public void vaddFP32WithReduceDimDelegatesWithoutParams() {
    byte[] blob = { 0, 1, 2, 3 };
    assertEquals(commands.vaddFP32("vs", blob, "el", 2, null),
      commands.vaddFP32("vs", blob, "el", 2));
  }

  @Test
  public void binaryVaddWithReduceDimDelegatesWithoutParams() {
    float[] vector = { 1.0f, 2.0f };
    byte[] key = { 'v' };
    byte[] element = { 'e' };
    assertEquals(commands.vadd(key, vector, element, 1, null),
      commands.vadd(key, vector, element, 1));
  }

  @Test
  public void binaryVaddFP32WithReduceDimDelegatesWithoutParams() {
    byte[] blob = { 0, 1, 2, 3 };
    byte[] key = { 'v' };
    byte[] element = { 'e' };
    assertEquals(commands.vaddFP32(key, blob, element, 1, null),
      commands.vaddFP32(key, blob, element, 1));
  }

  @Test
  public void vsimWithScoresDelegatesWithoutParams() {
    float[] vector = { 1.0f, 0.0f };
    assertEquals(commands.vsimWithScores("vs", vector, null),
      commands.vsimWithScores("vs", vector));
  }

  @Test
  public void vsimByElementWithScoresDelegatesWithoutParams() {
    assertEquals(commands.vsimByElementWithScores("vs", "el", null),
      commands.vsimByElementWithScores("vs", "el"));
  }

  @Test
  public void vsimWithScoresAndAttribsDelegatesWithoutParams() {
    float[] vector = { 1.0f, 0.0f };
    assertEquals(commands.vsimWithScoresAndAttribs("vs", vector, null),
      commands.vsimWithScoresAndAttribs("vs", vector));
  }

  @Test
  public void vsimByElementWithScoresAndAttribsDelegatesWithoutParams() {
    assertEquals(commands.vsimByElementWithScoresAndAttribs("vs", "el", null),
      commands.vsimByElementWithScoresAndAttribs("vs", "el"));
  }

  @Test
  public void binaryVsimWithScoresDelegatesWithoutParams() {
    float[] vector = { 1.0f, 0.0f };
    byte[] key = { 'v' };
    assertEquals(commands.vsimWithScores(key, vector, null), commands.vsimWithScores(key, vector));
  }

  @Test
  public void binaryVsimByElementWithScoresDelegatesWithoutParams() {
    byte[] key = { 'v' };
    byte[] element = { 'e' };
    assertEquals(commands.vsimByElementWithScores(key, element, null),
      commands.vsimByElementWithScores(key, element));
  }

  @Test
  public void binaryVsimWithScoresAndAttribsDelegatesWithoutParams() {
    float[] vector = { 1.0f, 0.0f };
    byte[] key = { 'v' };
    assertEquals(commands.vsimWithScoresAndAttribs(key, vector, null),
      commands.vsimWithScoresAndAttribs(key, vector));
  }

  @Test
  public void binaryVsimByElementWithScoresAndAttribsDelegatesWithoutParams() {
    byte[] key = { 'v' };
    byte[] element = { 'e' };
    assertEquals(commands.vsimByElementWithScoresAndAttribs(key, element, null),
      commands.vsimByElementWithScoresAndAttribs(key, element));
  }
}
