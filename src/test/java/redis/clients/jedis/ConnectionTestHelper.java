package redis.clients.jedis;

public class ConnectionTestHelper
{
  public static HostAndPort getHostAndPort(Connection connection) {
      return connection.getHostAndPort();
  }
}
