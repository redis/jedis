package redis.clients.jedis.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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

  @Test
  public void newVectorSetCommandFactoriesAreMarkedExperimental() throws IOException {
    String source = new String(
        Files.readAllBytes(Paths.get("src/main/java/redis/clients/jedis/CommandObjects.java")),
        StandardCharsets.UTF_8);
    assertExperimental(source,
      "public final CommandObject<Boolean> vadd(String key, float[] vector, String element, int reduceDim) {");
    assertExperimental(source,
      "public final CommandObject<Boolean> vaddFP32(String key, byte[] vectorBlob, String element, int reduceDim) {");
    assertExperimental(source,
      "public final CommandObject<Boolean> vadd(byte[] key, float[] vector, byte[] element, int reduceDim) {");
    assertExperimental(source,
      "public final CommandObject<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim) {");
    assertExperimental(source,
      "public final CommandObject<Map<String, Double>> vsimWithScores(String key, float[] vector) {");
    assertExperimental(source,
      "public final CommandObject<Map<String, VSimScoreAttribs>> vsimWithScoresAndAttribs(String key, float[] vector) {");
    assertExperimental(source,
      "public final CommandObject<Map<String, Double>> vsimByElementWithScores(String key, String element) {");
    assertExperimental(source,
      "public final CommandObject<Map<String, VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(String key, String element) {");
    assertExperimental(source,
      "public final CommandObject<Map<byte[], Double>> vsimWithScores(byte[] key, float[] vector) {");
    assertExperimental(source,
      "public final CommandObject<Map<byte[], VSimScoreAttribs>> vsimWithScoresAndAttribs(byte[] key, float[] vector) {");
    assertExperimental(source,
      "public final CommandObject<Map<byte[], Double>> vsimByElementWithScores(byte[] key, byte[] element) {");
    assertExperimental(source,
      "public final CommandObject<Map<byte[], VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element) {");
  }

  private void assertExperimental(String source, String methodDeclaration) {
    int methodIndex = source.indexOf(methodDeclaration);
    assertTrue(methodIndex >= 0, () -> methodDeclaration + " should exist");
    assertTrue(source.substring(0, methodIndex).trim().endsWith("@Experimental"),
      () -> methodDeclaration + " should be marked experimental");
  }
}
