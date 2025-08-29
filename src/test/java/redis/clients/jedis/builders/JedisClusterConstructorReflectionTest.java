package redis.clients.jedis.builders;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Reflection-based coverage test for JedisCluster constructors against builder API. The test
 * verifies each constructor parameter can be provided via: - ClusterClientBuilder methods, or -
 * DefaultJedisClientConfig.Builder methods, or - Custom ConnectionProvider (manual coverage)
 */
@EnabledIfSystemProperty(named = "with-param-names", matches = "true")
public class JedisClusterConstructorReflectionTest {

  private static final Logger log = LoggerFactory
      .getLogger(JedisClusterConstructorReflectionTest.class);

  @Test
  @DisplayName("Builder coverage of JedisCluster constructors (reflection)")
  void testConstructorParameterCoverageReport() {
    Constructor<?>[] ctors = JedisCluster.class.getConstructors();
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

        if (t == Set.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().nodes(Set<HostAndPort>)";
        } else if (t == HostAndPort.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().nodes(Set.of(HostAndPort))";
        } else if (t == JedisClientConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().clientConfig(DefaultJedisClientConfig...)";
        } else if (t == GenericObjectPoolConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().poolConfig(...)";
        } else if (t == Cache.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().cache(...)";
        } else if (t == CacheConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().cacheConfig(...)";
        } else if (t == Duration.class) {
          // Either maxTotalRetriesDuration or topologyRefreshPeriod
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisCluster.builder().maxTotalRetriesDuration(..)/topologyRefreshPeriod(..)";
        } else if (t == HostAndPortMapper.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().hostAndPortMapper(...)";
        } else if (t == int.class || t == Integer.class) {
          String lname = name.toLowerCase();
          if (lname.contains("attempt")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "JedisCluster.builder().maxAttempts(...)";
          } else if (lname.contains("port")) {
            // part of HostAndPort in string,int combos -> handled by string/host mapping below
            paramCoverageBy[i] = "JedisCluster.builder().nodes(Set.of(new HostAndPort(host,port)))";
          } else if (lname.contains("db") || lname.contains("database")) {
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
        } else if (t == String.class) {
          String lname = name.toLowerCase();
          if (lname.contains("host")) {
            paramCoverageBy[i] = "JedisCluster.builder().nodes(Set.of(new HostAndPort(host,port)))";
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
            // could be password; mark to client config
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().password/clientName/...";
          }
        } else if (t == SSLSocketFactory.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslSocketFactory(...)";
        } else if (t == SSLParameters.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslParameters(...)";
        } else if (t == HostnameVerifier.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().hostnameVerifier(...)";
        } else if (t == ConnectionProvider.class || t == ClusterConnectionProvider.class
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

      // Pair host/port pattern for (String,int,...) convenience ctors
      int hostIdx = findPotentialHostStringIndex(params);
      int portIdx = findPortIndex(params);
      if (hostIdx != -1 && portIdx != -1) {
        paramCovered[hostIdx] = true;
        if (paramCoverageBy[hostIdx] == null)
          paramCoverageBy[hostIdx] = "JedisCluster.builder().nodes(Set.of(new HostAndPort(host,port)))";
        paramCovered[portIdx] = true;
        if (paramCoverageBy[portIdx] == null)
          paramCoverageBy[portIdx] = "JedisCluster.builder().nodes(Set.of(new HostAndPort(host,port)))";
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

  // ===== Helpers copied from pooled test, adapted =====
  private static boolean hasType(java.lang.reflect.Parameter[] params, Class<?> type) {
    for (java.lang.reflect.Parameter p : params)
      if (p.getType() == type) return true;
    return false;
  }

  private static int findPortIndex(java.lang.reflect.Parameter[] params) {
    for (int i = 0; i < params.length; i++) {
      Class<?> t = params[i].getType();
      if (t == int.class || t == Integer.class) {
        String n = safeName(params[i]).toLowerCase();
        if (n.contains("port")) return i;
      }
    }
    int hostIdx = findPotentialHostStringIndex(params);
    if (hostIdx != -1) {
      for (int i = 0; i < params.length; i++)
        if (params[i].getType() == int.class || params[i].getType() == Integer.class) return i;
    }
    return -1;
  }

  private static int findPotentialHostStringIndex(java.lang.reflect.Parameter[] params) {
    for (int i = 0; i < params.length; i++) {
      if (params[i].getType() == String.class) {
        String n = safeName(params[i]).toLowerCase();
        if (n.contains("host")) return i;
      }
    }
    boolean hasInt = false;
    int stringCount = 0;
    int stringIdx = -1;
    for (int i = 0; i < params.length; i++) {
      Class<?> t = params[i].getType();
      if (t == int.class || t == Integer.class) hasInt = true;
      if (t == String.class) {
        stringCount++;
        stringIdx = i;
      }
    }
    if (hasInt && stringCount == 1) return stringIdx;
    return -1;
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
