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
    assertExperimentalIn("CommandObjects.java",
      "public final CommandObject<Boolean> vadd(String key, float[] vector, String element, int reduceDim) {",
      "public final CommandObject<Boolean> vaddFP32(String key, byte[] vectorBlob, String element, int reduceDim) {",
      "public final CommandObject<Boolean> vadd(byte[] key, float[] vector, byte[] element, int reduceDim) {",
      "public final CommandObject<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim) {",
      "public final CommandObject<Map<String, Double>> vsimWithScores(String key, float[] vector) {",
      "public final CommandObject<Map<String, VSimScoreAttribs>> vsimWithScoresAndAttribs(String key, float[] vector) {",
      "public final CommandObject<Map<String, Double>> vsimByElementWithScores(String key, String element) {",
      "public final CommandObject<Map<String, VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(String key, String element) {",
      "public final CommandObject<Map<byte[], Double>> vsimWithScores(byte[] key, float[] vector) {",
      "public final CommandObject<Map<byte[], VSimScoreAttribs>> vsimWithScoresAndAttribs(byte[] key, float[] vector) {",
      "public final CommandObject<Map<byte[], Double>> vsimByElementWithScores(byte[] key, byte[] element) {",
      "public final CommandObject<Map<byte[], VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element) {");
  }

  @Test
  public void newVectorSetConcreteOverridesAreMarkedExperimental() throws IOException {
    assertExperimentalIn("Jedis.java",
      "public boolean vadd(String key, float[] vector, String element, int reduceDim) {",
      "public boolean vaddFP32(String key, byte[] vectorBlob, String element, int reduceDim) {",
      "public Map<String, Double> vsimWithScores(String key, float[] vector) {",
      "public Map<String, VSimScoreAttribs> vsimWithScoresAndAttribs(String key, float[] vector) {",
      "public Map<String, Double> vsimByElementWithScores(String key, String element) {",
      "public Map<String, VSimScoreAttribs> vsimByElementWithScoresAndAttribs(String key, String element) {",
      "public boolean vadd(byte[] key, float[] vector, byte[] element, int reduceDim) {",
      "public boolean vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim) {",
      "public Map<byte[], Double> vsimWithScores(byte[] key, float[] vector) {",
      "public Map<byte[], VSimScoreAttribs> vsimWithScoresAndAttribs(byte[] key, float[] vector) {",
      "public Map<byte[], Double> vsimByElementWithScores(byte[] key, byte[] element) {",
      "public Map<byte[], VSimScoreAttribs> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element) {");
    assertExperimentalIn("UnifiedJedis.java",
      "public boolean vadd(String key, float[] vector, String element, int reduceDim) {",
      "public boolean vaddFP32(String key, byte[] vectorBlob, String element, int reduceDim) {",
      "public Map<String, Double> vsimWithScores(String key, float[] vector) {",
      "public Map<String, VSimScoreAttribs> vsimWithScoresAndAttribs(String key, float[] vector) {",
      "public Map<String, Double> vsimByElementWithScores(String key, String element) {",
      "public Map<String, VSimScoreAttribs> vsimByElementWithScoresAndAttribs(String key, String element) {",
      "public boolean vadd(byte[] key, float[] vector, byte[] element, int reduceDim) {",
      "public boolean vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim) {",
      "public Map<byte[], Double> vsimWithScores(byte[] key, float[] vector) {",
      "public Map<byte[], VSimScoreAttribs> vsimWithScoresAndAttribs(byte[] key, float[] vector) {",
      "public Map<byte[], Double> vsimByElementWithScores(byte[] key, byte[] element) {",
      "public Map<byte[], VSimScoreAttribs> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element) {");
    assertExperimentalIn("PipeliningBase.java",
      "public Response<Boolean> vadd(String key, float[] vector, String element, int reduceDim) {",
      "public Response<Boolean> vaddFP32(String key, byte[] vectorBlob, String element, int reduceDim) {",
      "public Response<Map<String, Double>> vsimWithScores(String key, float[] vector) {",
      "public Response<Map<String, VSimScoreAttribs>> vsimWithScoresAndAttribs(String key, float[] vector) {",
      "public Response<Map<String, VSimScoreAttribs>> vsimWithScoresAndAttribs(String key, float[] vector, VSimParams params) {",
      "public Response<Map<String, Double>> vsimByElementWithScores(String key, String element) {",
      "public Response<Map<String, VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(String key, String element) {",
      "public Response<Map<String, VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(String key, String element, VSimParams params) {",
      "public Response<Boolean> vadd(byte[] key, float[] vector, byte[] element, int reduceDim) {",
      "public Response<Boolean> vaddFP32(byte[] key, byte[] vectorBlob, byte[] element, int reduceDim) {",
      "public Response<Map<byte[], Double>> vsimWithScores(byte[] key, float[] vector) {",
      "public Response<Map<byte[], VSimScoreAttribs>> vsimWithScoresAndAttribs(byte[] key, float[] vector) {",
      "public Response<Map<byte[], VSimScoreAttribs>> vsimWithScoresAndAttribs(byte[] key, float[] vector, VSimParams params) {",
      "public Response<Map<byte[], Double>> vsimByElementWithScores(byte[] key, byte[] element) {",
      "public Response<Map<byte[], VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element) {",
      "public Response<Map<byte[], VSimScoreAttribs>> vsimByElementWithScoresAndAttribs(byte[] key, byte[] element, VSimParams params) {");
  }

  private void assertExperimentalIn(String fileName, String... methodDeclarations)
      throws IOException {
    String source = new String(
        Files.readAllBytes(Paths.get("src/main/java/redis/clients/jedis", fileName)),
        StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    for (String methodDeclaration : methodDeclarations) {
      assertExperimental(source, methodDeclaration);
    }
  }

  private void assertExperimental(String source, String methodDeclaration) {
    String normalizedMethodDeclaration = methodDeclaration.replaceAll("\\s+", " ");
    int methodIndex = source.indexOf(normalizedMethodDeclaration);
    assertTrue(methodIndex >= 0, () -> normalizedMethodDeclaration + " should exist");
    assertTrue(source.substring(0, methodIndex).trim().endsWith("@Experimental"),
      () -> normalizedMethodDeclaration + " should be marked experimental");
  }
}
