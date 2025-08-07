package redis.clients.jedis.util;

import java.time.Duration;
import java.time.Instant;

/**
 * A generic wrapper that holds a value with an expiration time.
 * Useful for caching values that should be automatically invalidated after a certain period.
 *
 * @param <T> the type of the value being wrapped
 */
public class Expirable<T> {
    
    private final T value;
    private final Instant expirationTime;
    
    /**
     * Creates an expirable value that expires after the specified duration from now.
     *
     * @param value the value to wrap
     * @param duration the duration after which the value should expire
     */
    public Expirable(T value, Duration duration) {
        this(value, Instant.now().plus(duration));
    }
    
    /**
     * Creates an expirable value that expires at the specified time.
     *
     * @param value the value to wrap
     * @param expirationTime the time at which the value should expire
     */
    public Expirable(T value, Instant expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }
    
    /**
     * Gets the wrapped value.
     *
     * @return the wrapped value
     */
    public T getValue() {
        return value;
    }
    
    /**
     * Gets the expiration time.
     *
     * @return the expiration time
     */
    public Instant getExpirationTime() {
        return expirationTime;
    }
    
    /**
     * Checks if the value has expired.
     *
     * @return true if the value has expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * Checks if the value is still valid (not expired).
     *
     * @return true if the value is still valid, false if expired
     */
    public boolean isValid() {
        return !isExpired();
    }
    
    @Override
    public String toString() {
        return String.format("Expirable{value=%s, expirationTime=%s, expired=%s}", 
                           value, expirationTime, isExpired());
    }
}
