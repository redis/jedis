package redis.clients.jedis.builders;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisSentineled;
import redis.clients.jedis.ReadFrom;
import redis.clients.jedis.ReadOnlyPredicate;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.SentineledConnectionProvider;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Constructor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reflection-based coverage test for JedisSentineled constructors against builder API.
 */
@EnabledIfSystemProperty(named = "with-param-names", matches = "true")
public class JedisSentineledConstructorReflectionTest {

  private static final Logger log = LoggerFactory
      .getLogger(JedisSentineledConstructorReflectionTest.class);

  @Test
  @DisplayName("Builder coverage of JedisSentineled constructors (reflection)")
  void testConstructorParameterCoverageReport() {
    Constructor<?>[] ctors = JedisSentineled.class.getConstructors();
    int total = 0, covered = 0;

    StringBuilder uncoveredReport = new StringBuilder();

    for (Constructor<?> ctor : ctors) {
      total++;
      java.lang.reflect.Parameter[] params = ctor.getParameters();

      boolean[] paramCovered = new boolean[params.length];
      String[] paramCoverageBy = new String[params.length];
      String[] paramWhyMissing = new String[params.length];

      for (int i = 0; i < params.length; i++) {
        java.lang.reflect.Parameter p = params[i];
        Class<?> t = p.getType();
        String name = safeName(p);

        if (t == String.class) {
          String lname = name.toLowerCase();
          if (lname.contains("master")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "JedisSentineled.builder().masterName(String)";
          } else if (lname.contains("user")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().user(...)";
          } else if (lname.contains("pass")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().password(...)";
          } else if (lname.contains("client") && lname.contains("name")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().clientName(...)";
          } else {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().password/clientName/...";
          }
        } else if (t == Set.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().sentinels(Set<HostAndPort>)";
        } else if (t == JedisClientConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().masterClientConfig(...)/sentinelClientConfig(...)";
        } else if (t == GenericObjectPoolConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().poolConfig(...)";
        } else if (t == Cache.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().cache(...)";
        } else if (t == CacheConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().cacheConfig(...)";
        } else if (t == ReadFrom.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisSentineled.builder().readForm(...)";
        } else if (t == ReadOnlyPredicate.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().readOnlyPredicate(...)";
        } else if (t == int.class || t == Integer.class) {
          String lname = name.toLowerCase();
          if (lname.contains("db") || lname.contains("database")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().database(...)";
          } else if (lname.contains("conn")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().connectionTimeoutMillis(...)";
          } else if ((lname.contains("so") && lname.contains("timeout"))
              || lname.equals("sockettimeout")) {
                paramCovered[i] = true;
                paramCoverageBy[i] = "DefaultJedisClientConfig.builder().socketTimeoutMillis(...)";
              } else
            if (lname.contains("infinite")) {
              paramCovered[i] = true;
              paramCoverageBy[i] = "DefaultJedisClientConfig.builder().blockingSocketTimeoutMillis(...)";
            } else if (lname.contains("timeout")) {
              paramCovered[i] = true;
              paramCoverageBy[i] = "DefaultJedisClientConfig.builder().connectionTimeoutMillis(...)+socketTimeoutMillis(...)";
            }
        } else if (t == boolean.class || t == Boolean.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().ssl(...)";
        } else if (t == SSLSocketFactory.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslSocketFactory(...)";
        } else if (t == SSLParameters.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslParameters(...)";
        } else if (t == HostnameVerifier.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().hostnameVerifier(...)";
        } else if (t == ConnectionProvider.class || t == SentineledConnectionProvider.class
            || t == PooledObjectFactory.class) {
              paramCovered[i] = true;
              paramCoverageBy[i] = "Custom ConnectionProvider via builder.connectionProvider(...)";
            } else
          if (t == CommandExecutor.class || t == CommandObjects.class) {
            paramCovered[i] = true;
            paramCoverageBy[i] = t == CommandExecutor.class ? "builder.commandExecutor(...)"
                : "Internal (builder-provided CommandObjects)";
          } else if (t == RedisProtocol.class) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "builder.redisProtocol(...)";
          } else {
            paramCovered[i] = false;
            paramWhyMissing[i] = "No known builder mapping for type: " + t.getSimpleName();
          }
      }

      boolean ctorCovered = true;
      StringBuilder missingParams = new StringBuilder();
      for (int i = 0; i < params.length; i++) {
        if (!paramCovered[i]) {
          ctorCovered = false;
          missingParams.append("  - ").append(prettyParam(params[i])).append(" -> ")
              .append(paramWhyMissing[i] != null ? paramWhyMissing[i] : "unknown").append("\n");
        }
      }

      if (!ctorCovered) {
        uncoveredReport.append("\nUncovered constructor: ").append(prettySignature(ctor))
            .append("\n");
        uncoveredReport.append("Missing parameters:\n").append(missingParams);
      } else {
        covered++;
      }
    }

    log.info("Analyzed {} constructors; fully covered: {}", total, covered);
    if (covered < total) {
      log.warn("Uncovered constructors detected:{}", uncoveredReport);
    }

    assertEquals(total, covered, "Expected all constructors to be covered by builders");
    assertTrue(total > 0, "Expected at least one constructor to analyze");
  }

  private static String prettySignature(Constructor<?> ctor) {
    StringBuilder sb = new StringBuilder();
    sb.append(ctor.getDeclaringClass().getSimpleName()).append('(');
    java.lang.reflect.Parameter[] ps = ctor.getParameters();
    for (int i = 0; i < ps.length; i++) {
      if (i > 0) sb.append(", ");
      sb.append(prettyParam(ps[i]));
    }
    sb.append(')');
    return sb.toString();
  }

  private static String prettyParam(java.lang.reflect.Parameter p) {
    return p.getType().getSimpleName() + " " + safeName(p);
  }

  private static String safeName(java.lang.reflect.Parameter p) {
    try {
      String n = p.getName();
      return n != null ? n : ("arg" + p.getParameterizedType().getTypeName().hashCode());
    } catch (Exception e) {
      return "arg";
    }
  }
}
