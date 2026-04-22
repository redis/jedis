package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.util.JedisURIHelper.*;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisProtocol;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

public class JedisURIHelperTest {

  @Test
  public void shouldGetUserAndPasswordFromURIWithCredentials() throws URISyntaxException {
    URI uri = new URI("redis://user:password@host:9000/0");
    assertEquals("user", JedisURIHelper.getUser(uri));
    assertEquals("password", JedisURIHelper.getPassword(uri));
  }

  @Test
  public void shouldGetNullUserFromURIWithCredentials() throws URISyntaxException {
    URI uri = new URI("redis://:password@host:9000/0");
    assertNull(JedisURIHelper.getUser(uri));
    assertEquals("password", JedisURIHelper.getPassword(uri));
  }

  @Test
  public void shouldReturnNullIfURIDoesNotHaveCredentials() throws URISyntaxException {
    URI uri = new URI("redis://host:9000/0");
    assertNull(JedisURIHelper.getUser(uri));
    assertNull(JedisURIHelper.getPassword(uri));
  }

  @Test
  public void shouldGetDbFromURIWithCredentials() throws URISyntaxException {
    URI uri = new URI("redis://user:password@host:9000/3");
    assertEquals(3, JedisURIHelper.getDBIndex(uri));
  }

  @Test
  public void shouldGetDbFromURIWithoutCredentials() throws URISyntaxException {
    URI uri = new URI("redis://host:9000/4");
    assertEquals(4, JedisURIHelper.getDBIndex(uri));
  }

  @Test
  public void shouldGetDefaultDbFromURIIfNoDbWasSpecified() throws URISyntaxException {
    URI uri = new URI("redis://host:9000");
    assertEquals(0, JedisURIHelper.getDBIndex(uri));
  }

  @Test
  public void hasDbIndex_shouldReturnFalseIfURIDoesNotHaveDbIndex() throws URISyntaxException {
    URI uri = new URI("redis://host:9000");
    assertFalse(JedisURIHelper.hasDbIndex(uri));
  }

  @Test
  public void hasDbIndex_shouldReturnTrueIfURIDoesHaveDbIndex() throws URISyntaxException {
    URI uri = new URI("redis://host:9000/3");
    assertTrue(JedisURIHelper.hasDbIndex(uri));
  }

  @Test
  public void shouldValidateInvalidURIs() throws URISyntaxException {
    assertFalse(JedisURIHelper.isValid(new URI("host:9000")));
    assertFalse(JedisURIHelper.isValid(new URI("user:password@host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("redis://host/0")));
  }

  @Test
  public void shouldRejectNonRedisSchemes() throws URISyntaxException {
    assertFalse(JedisURIHelper.isValid(new URI("http://host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("https://user:password@host:6380/0")));
    assertFalse(JedisURIHelper.isValid(new URI("ftp://host:9000")));
    assertTrue(JedisURIHelper.isValid(new URI("redis://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("rediss://host:9000/0")));
  }

  @Test
  public void shouldAcceptMixedCaseSchemes() throws URISyntaxException {
    // URI schemes are case-insensitive per RFC 3986
    assertTrue(JedisURIHelper.isValid(new URI("Redis://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("REDIS://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("ReDiS://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("Rediss://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("REDISS://host:9000/0")));
    assertTrue(JedisURIHelper.isValid(new URI("ReDiSs://host:9000/0")));
  }

  @Test
  public void shouldRejectTyposInScheme() throws URISyntaxException {
    assertFalse(JedisURIHelper.isValid(new URI("redi://host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("rediss-cluster://host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("redis-sentinel://host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("redisss://host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("redis-ssl://host:9000/0")));
  }

  @Test
  public void shouldGetDefaultProtocolWhenNotDefined() {
    assertNull(getRedisProtocol(URI.create("redis://host:1234")));
    assertNull(getRedisProtocol(URI.create("redis://host:1234/1")));
  }

  @Test
  public void shouldGetProtocolFromDefinition() {
    assertEquals(RedisProtocol.RESP3, getRedisProtocol(URI.create("redis://host:1234?protocol=3")));
    assertEquals(RedisProtocol.RESP3,
      getRedisProtocol(URI.create("redis://host:1234/?protocol=3")));
    assertEquals(RedisProtocol.RESP3,
      getRedisProtocol(URI.create("redis://host:1234/1?protocol=3")));
    assertEquals(RedisProtocol.RESP3,
      getRedisProtocol(URI.create("redis://host:1234/1/?protocol=3")));
  }

  @Test
  public void emptyPassword() {
    // ensure we can provide an empty password for default user
    assertThat(JedisURIHelper.getPassword(URI.create("redis://:@host:9000/0")), emptyString());

    // ensure we can provide an empty password for user
    assertEquals(JedisURIHelper.getUser(URI.create("redis://username:@host:9000/0")), "username");
    assertThat(JedisURIHelper.getPassword(URI.create("redis://username:@host:9000/0")),
      emptyString());
  }

  @Test
  public void shouldThrowIfNoPasswordInURI() throws URISyntaxException {
    // ensure we throw if user is provided but password is missing in URI
    URI uri = new URI("redis://user@host:9000/0");
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> getPassword(uri));
    assertEquals("Password not provided in uri.", illegalArgumentException.getMessage());
  }

  @Test
  public void isRedisScheme_shouldBeCaseInsensitive() throws URISyntaxException {
    assertTrue(JedisURIHelper.isRedisScheme(new URI("redis://host:9000")));
    assertTrue(JedisURIHelper.isRedisScheme(new URI("Redis://host:9000")));
    assertTrue(JedisURIHelper.isRedisScheme(new URI("REDIS://host:9000")));
    assertTrue(JedisURIHelper.isRedisScheme(new URI("ReDiS://host:9000")));

    assertFalse(JedisURIHelper.isRedisScheme(new URI("rediss://host:9000")));
    assertFalse(JedisURIHelper.isRedisScheme(new URI("http://host:9000")));
  }

  @Test
  public void isRedisSSLScheme_shouldBeCaseInsensitive() throws URISyntaxException {
    assertTrue(JedisURIHelper.isRedisSSLScheme(new URI("rediss://host:9000")));
    assertTrue(JedisURIHelper.isRedisSSLScheme(new URI("Rediss://host:9000")));
    assertTrue(JedisURIHelper.isRedisSSLScheme(new URI("REDISS://host:9000")));
    assertTrue(JedisURIHelper.isRedisSSLScheme(new URI("ReDiSs://host:9000")));

    assertFalse(JedisURIHelper.isRedisSSLScheme(new URI("redis://host:9000")));
    assertFalse(JedisURIHelper.isRedisSSLScheme(new URI("https://host:9000")));
  }
}
