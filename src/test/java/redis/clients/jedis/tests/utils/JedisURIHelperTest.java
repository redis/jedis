package redis.clients.jedis.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import redis.clients.util.JedisURIHelper;

public class JedisURIHelperTest {

  @Test
  public void shouldGetPasswordFromURIWithCredentials() throws URISyntaxException {
    URI uri = new URI("redis://user:password@host:9000/0");
    assertEquals("password", JedisURIHelper.getPassword(uri));
  }

  @Test
  public void shouldReturnNullIfURIDoesNotHaveCredentials() throws URISyntaxException {
    URI uri = new URI("redis://host:9000/0");
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
  public void shouldValidateInvalidURIs() throws URISyntaxException {
    assertFalse(JedisURIHelper.isValid(new URI("host:9000")));
    assertFalse(JedisURIHelper.isValid(new URI("user:password@host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("host:9000/0")));
    assertFalse(JedisURIHelper.isValid(new URI("redis://host/0")));
  }

}
