package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.RedisCredentials;

public class RedisRestAPIUnitTest {

  static class TestEndpoint implements Endpoint {
    @Override
    public String getHost() {
      return "localhost";
    }

    @Override
    public int getPort() {
      return 8443;
    }
  }

  @Test
  void getBdbs_parsesArrayOfObjects() throws Exception {
    RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
    HttpURLConnection conn = mock(HttpURLConnection.class);
    doReturn(conn).when(api).createConnection(any(), any(), any());

    when(conn.getResponseCode()).thenReturn(200);
    String body = "[ {\"uid\":\"1\"}, {\"uid\":\"2\"} ]";
    when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));

    assertEquals(java.util.Arrays.asList("1", "2"), api.getBdbs());
    verify(conn, times(1)).disconnect();
  }

  @Test
  void availability_logsAndReturnsFalseForNon200() throws Exception {
    RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
    HttpURLConnection conn = mock(HttpURLConnection.class);
    doReturn(conn).when(api).createConnection(any(), any(), any());

    when(conn.getResponseCode()).thenReturn(503);
    String body = "{\"error_code\":\"bdb_unavailable\",\"description\":\"Database is not available\"}";
    when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream(body.getBytes()));

    assertFalse(api.checkBdbAvailability("2", false));
  }

  private static Supplier<RedisCredentials> creds() {
    return () -> new DefaultRedisCredentials("testUser", "testPwd");
  }

  @Test
  void availability_200_and_503_paths_cover_lagAware_toggle() throws Exception {
    RedisRestAPI api = spy(new RedisRestAPI(new TestEndpoint(), creds(), 1000));
    HttpURLConnection conn = mock(HttpURLConnection.class);
    doReturn(conn).when(api).createConnection(any(), any(), any());

    // Healthy path (200)
    when(conn.getResponseCode()).thenReturn(200);
    assertTrue(api.checkBdbAvailability("123", true));

    // Unhealthy path (503) with error body
    reset(conn);
    doReturn(conn).when(api).createConnection(any(), any(), any());
    when(conn.getResponseCode()).thenReturn(503);
    when(conn.getErrorStream()).thenReturn(new ByteArrayInputStream("{\"error_code\":\"bdb_unavailable\"}".getBytes()));
    assertFalse(api.checkBdbAvailability("123", false));
  }
}
