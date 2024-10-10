package redis.clients.jedis;

public interface RedisCredentials {

  /**
   * @return Redis ACL user
   */
  default String getUser() {
    return null;
  }

  default char[] getPassword() {
    return null;
  }
}
