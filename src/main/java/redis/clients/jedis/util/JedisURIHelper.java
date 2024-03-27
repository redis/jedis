package redis.clients.jedis.util;

import java.net.URI;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.csc.CaffeineClientSideCache;
import redis.clients.jedis.csc.ClientSideCache;
import redis.clients.jedis.csc.GuavaClientSideCache;

public final class JedisURIHelper {

  private static final String REDIS = "redis";
  private static final String REDISS = "rediss";

  private JedisURIHelper() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static HostAndPort getHostAndPort(URI uri) {
    return new HostAndPort(uri.getHost(), uri.getPort());
  }

  public static String getUser(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      String user = userInfo.split(":", 2)[0];
      if (user.isEmpty()) {
        user = null; // return null user is not specified
      }
      return user;
    }
    return null;
  }

  public static String getPassword(URI uri) {
    String userInfo = uri.getUserInfo();
    if (userInfo != null) {
      return userInfo.split(":", 2)[1];
    }
    return null;
  }

  public static int getDBIndex(URI uri) {
    String[] pathSplit = uri.getPath().split("/", 2);
    if (pathSplit.length > 1) {
      String dbIndexStr = pathSplit[1];
      if (dbIndexStr.isEmpty()) {
        return Protocol.DEFAULT_DATABASE;
      }
      return Integer.parseInt(dbIndexStr);
    } else {
      return Protocol.DEFAULT_DATABASE;
    }
  }

  public static RedisProtocol getRedisProtocol(URI uri) {
    if (uri.getQuery() == null) return null;

    String[] params = uri.getQuery().split("&");
    for (String param : params) {
      int idx = param.indexOf("=");
      if (idx < 0) continue;
      if ("protocol".equals(param.substring(0, idx))) {
        String ver = param.substring(idx + 1);
        for (RedisProtocol proto : RedisProtocol.values()) {
          if (proto.version().equals(ver)) {
            return proto;
          }
        }
        throw new IllegalArgumentException("Unknown protocol " + ver);
      }
    }
    return null; // null (default) when not defined
  }

  private static final Integer ZERO_INTEGER = 0;

  @Experimental
  public static ClientSideCache getClientSideCache(URI uri) {
    if (uri.getQuery() == null) return null;

    boolean guava = false, caffeine = false; // cache_lib
    Integer maxSize = null; // cache_max_size --> 0 = disbale
    Integer ttl = null; // cache_ttl --> 0 = no ttl
    // cache-max-idle

    String[] params = uri.getQuery().split("&");
    for (String param : params) {
      int idx = param.indexOf("=");
      if (idx < 0) continue;

      String key = param.substring(0, idx);
      String val = param.substring(idx + 1);

      switch (key) {

        case "cache_lib":
          switch (val) {
            case "guava":
              guava = true;
              break;
            case "caffeine":
              caffeine = true;
              break;
            default:
              throw new IllegalArgumentException("Unsupported library " + val);
          }
          break;

        case "cache_max_size":
          try {
            maxSize = Integer.parseInt(val);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Value of cache_max_size must be an integer.", nfe);
          }
          break;

        case "cache_ttl":
          try {
            ttl = Integer.parseInt(val);
          } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Value of cache_ttl must be an integer denoting seconds.", nfe);
          }
          break;
      }
    }

    // special cases
    if (ZERO_INTEGER.equals(maxSize)) {
      return null;
    }
    if (!guava && !caffeine && (maxSize != null || ttl != null)) {
      throw new IllegalArgumentException("The cache library (guava OR caffeine) must be selected.");
    }
    if (ZERO_INTEGER.equals(ttl)) {
      ttl = null; // below, only null will be checked
    }

    if (guava) {
      GuavaClientSideCache.Builder guavaBuilder = GuavaClientSideCache.builder();
      if (maxSize != null) guavaBuilder.maximumSize(maxSize);
      if (ttl != null) guavaBuilder.ttl(ttl);
      return guavaBuilder.build();
    } else if (caffeine) {
      CaffeineClientSideCache.Builder caffeineBuilder = CaffeineClientSideCache.builder();
      if (maxSize != null) caffeineBuilder.maximumSize(maxSize);
      if (ttl != null) caffeineBuilder.ttl(ttl);
      return caffeineBuilder.build();
    }

    return null; // null (default) when not defined
  }

  public static boolean isValid(URI uri) {
    if (isEmpty(uri.getScheme()) || isEmpty(uri.getHost()) || uri.getPort() == -1) {
      return false;
    }

    return true;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().length() == 0;
  }

  public static boolean isRedisScheme(URI uri) {
    return REDIS.equals(uri.getScheme());
  }

  public static boolean isRedisSSLScheme(URI uri) {
    return REDISS.equals(uri.getScheme());
  }

}
