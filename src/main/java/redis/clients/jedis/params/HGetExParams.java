package redis.clients.jedis.params;

/**
 * HGetExParams is a parameter class used when getting values of given keys in hash data with 
 * optionally setting/changing expiry time in Redis.
 * It can be used to set the expiry time of the key in seconds or milliseconds.
 * 
 * <p>This class includes the following methods:</p>
 * <ul>
 *   <li>{@link #hGetExParams()} - Static factory method to create a new instance of HGetExParams.</li>
 *   <li>{@link #ex(long)} - Set the specified expire time, in seconds.</li>
 *   <li>{@link #px(long)} - Set the specified expire time, in milliseconds.</li>
 *   <li>{@link #exAt(long)} - Set the specified Unix(epoch) time at which the key will expire, in seconds.</li>
 *   <li>{@link #pxAt(long)} - Set the specified Unix(epoch) time at which the key will expire, in milliseconds.</li>
 *   <li>{@link #persist()} - Remove the time-to-live associated with the key.</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * HGetExParams params = HGetExParams.hGetExParams().persist();
 * }
 * </pre>
 * 
 * @see BaseGetExParams
 */
public class HGetExParams extends BaseGetExParams<HGetExParams> {

  public static HGetExParams hGetExParams() {
    return new HGetExParams();
  }
}
