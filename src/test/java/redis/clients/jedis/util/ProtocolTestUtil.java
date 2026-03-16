package redis.clients.jedis.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

/**
 * Utility class for testing Redis protocol commands and serialization.
 * <p>
 * Provides helper methods to capture and inspect the RESP protocol output that would be sent to
 * Redis servers.
 * </p>
 */
public final class ProtocolTestUtil {

  private ProtocolTestUtil() {
    throw new InstantiationError("Must not instantiate this class");
  }

  /**
   * Captures the RESP protocol output that would be sent to Redis.
   * <p>
   * This method serializes the CommandArguments using the actual Protocol.sendCommand() method and
   * returns the raw RESP protocol string that would be sent over the wire.
   * </p>
   * @param args the command arguments to serialize
   * @return the RESP protocol string (e.g., "*3\r\n$6\r\nZRANGE\r\n$10\r\n3000000000\r\n...")
   * @throws AssertionError if an I/O error occurs during serialization (should never happen with
   *           ByteArrayOutputStream)
   */
  public static String captureCommandOutput(CommandArguments args) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    RedisOutputStream ros = new RedisOutputStream(baos);

    try {
      Protocol.sendCommand(ros, args);
      ros.flush();
    } catch (IOException e) {
      // This should never happen with ByteArrayOutputStream, but if it does, it's a test setup
      // error
      throw new AssertionError("Failed to serialize command arguments", e);
    }

    return baos.toString();
  }
}
