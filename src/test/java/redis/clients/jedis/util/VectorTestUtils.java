package redis.clients.jedis.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for vector-related test operations. Provides methods for converting between float
 * arrays and FP32 byte representations.
 */
public class VectorTestUtils {

  /**
   * Convert float array to FP32 byte blob (IEEE 754 format). Each float is converted to 4 bytes in
   * little-endian order.
   * @param floats the float array to convert
   * @return byte array containing FP32 representation
   */
  public static byte[] floatArrayToFP32Bytes(float[] floats) {
    byte[] bytes = new byte[floats.length * 4]; // 4 bytes per float
    for (int i = 0; i < floats.length; i++) {
      int bits = Float.floatToIntBits(floats[i]);
      bytes[i * 4] = (byte) (bits & 0xFF);
      bytes[i * 4 + 1] = (byte) ((bits >> 8) & 0xFF);
      bytes[i * 4 + 2] = (byte) ((bits >> 16) & 0xFF);
      bytes[i * 4 + 3] = (byte) ((bits >> 24) & 0xFF);
    }
    return bytes;
  }

  /**
   * Convert FP32 byte blob back to float array (IEEE 754 format). Uses ByteBuffer for clean and
   * readable conversion from little-endian bytes.
   * @param fp32Bytes the FP32 byte array to convert
   * @return List of Float values reconstructed from the byte data
   */
  public static List<Float> fp32BytesToFloatArray(byte[] fp32Bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(fp32Bytes).order(ByteOrder.LITTLE_ENDIAN);
    List<Float> floats = new ArrayList<>();
    while (buffer.remaining() >= 4) {
      floats.add(buffer.getFloat());
    }
    return floats;
  }

}
