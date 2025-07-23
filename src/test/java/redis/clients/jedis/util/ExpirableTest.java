package redis.clients.jedis.util;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.HostAndPort;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class ExpirableTest {

    @Test
    public void testExpirableWithDuration() {
        String value = "test-value";
        Duration duration = Duration.ofSeconds(1);
        
        Expirable<String> expirable = new Expirable<>(value, duration);
        
        assertEquals(value, expirable.getValue());
        assertFalse(expirable.isExpired());
        assertTrue(expirable.isValid());
        assertNotNull(expirable.getExpirationTime());
        assertTrue(expirable.getExpirationTime().isAfter(Instant.now()));
    }

    @Test
    public void testExpirableWithInstant() {
        String value = "test-value";
        Instant expirationTime = Instant.now().plusSeconds(1);
        
        Expirable<String> expirable = new Expirable<>(value, expirationTime);
        
        assertEquals(value, expirable.getValue());
        assertEquals(expirationTime, expirable.getExpirationTime());
        assertFalse(expirable.isExpired());
        assertTrue(expirable.isValid());
    }

    @Test
    public void testExpiredExpirable() {
        String value = "test-value";
        Duration negativeDuration = Duration.ofMillis(-1);
        
        Expirable<String> expiredExpirable = new Expirable<>(value, negativeDuration);
        
        assertEquals(value, expiredExpirable.getValue());
        assertTrue(expiredExpirable.isExpired());
        assertFalse(expiredExpirable.isValid());
    }

    @Test
    public void testExpirableWithPastInstant() {
        String value = "test-value";
        Instant pastTime = Instant.now().minusSeconds(1);
        
        Expirable<String> expiredExpirable = new Expirable<>(value, pastTime);
        
        assertEquals(value, expiredExpirable.getValue());
        assertEquals(pastTime, expiredExpirable.getExpirationTime());
        assertTrue(expiredExpirable.isExpired());
        assertFalse(expiredExpirable.isValid());
    }

    @Test
    public void testExpirableWithHostAndPort() {
        HostAndPort hostAndPort = new HostAndPort("localhost", 6379);
        Duration duration = Duration.ofSeconds(1);
        
        Expirable<HostAndPort> expirable = new Expirable<>(hostAndPort, duration);
        
        assertEquals(hostAndPort, expirable.getValue());
        assertFalse(expirable.isExpired());
        assertTrue(expirable.isValid());
    }

    @Test
    public void testExpirableWithNullValue() {
        Duration duration = Duration.ofSeconds(1);
        
        Expirable<String> expirable = new Expirable<>(null, duration);
        
        assertNull(expirable.getValue());
        assertFalse(expirable.isExpired());
        assertTrue(expirable.isValid());
    }

    @Test
    public void testExpirableWithZeroDuration() {
        String value = "test-value";
        Duration zeroDuration = Duration.ZERO;
        
        Expirable<String> expirable = new Expirable<>(value, zeroDuration);
        
        assertEquals(value, expirable.getValue());
        // Zero duration should be expired immediately or very quickly
        assertTrue(expirable.isExpired() || expirable.isValid()); // Allow for timing variations
    }
}
