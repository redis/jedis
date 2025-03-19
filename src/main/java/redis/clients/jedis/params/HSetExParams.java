package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

/**
 * HSetExParams is a parameter class used when setting key-value pairs of hash data with 
 * optional expiry and existance conditions in Redis.
 * It provides methods to specify whether the key should be set
 * only if it already exists or only if it does not already exist.
 * It can also be used to set the expiry time of the key in seconds or milliseconds.
 * 
 * <p>This class includes the following methods:</p>
 * <ul>
 *   <li>{@link #hSetExParams()} - Static factory method to create a new instance of HSetExParams.</li>
 *   <li>{@link #fnx()} - Sets the condition to only set the key if it does not already exist.</li>
 *   <li>{@link #fxx()} - Sets the condition to only set the key if it already exists.</li>
 *   <li>{@link #ex(long)} - Set the specified expire time, in seconds.</li>
 *   <li>{@link #px(long)} - Set the specified expire time, in milliseconds.</li>
 *   <li>{@link #exAt(long)} - Set the specified Unix(epoch) time at which the key will expire, in seconds.</li>
 *   <li>{@link #pxAt(long)} - Set the specified Unix(epoch) time at which the key will expire, in milliseconds.</li>
 *   <li>{@link #keepTtl()} - Retain the time to live associated with the key.</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * HSetExParams params = HSetExParams.hSetExParams().fnx();
 * }
 * </pre>
 * 
 * @see BaseSetExParams
 */
public class HSetExParams extends BaseSetExParams<HSetExParams> {

    private Keyword existance;

    public static HSetExParams hSetExParams() {
        return new HSetExParams();
    }

    /**
     * Only set the key if it does not already exist.
     * @return HSetExParams
     */
    public HSetExParams fnx() {
        this.existance = Keyword.FNX;
        return this;
    }

    /**
     * Only set the key if it already exist.
     * @return HSetExParams
     */
    public HSetExParams fxx() {
        this.existance = Keyword.FXX;
        return this;
    }

    @Override
    public void addParams(CommandArguments args) {
        if (existance != null) {
            args.add(existance);
        }

        super.addParams(args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HSetExParams setParams = (HSetExParams) o;
        return Objects.equals(existance, setParams.existance) && super.equals((BaseSetExParams) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(existance, super.hashCode());
    }

}