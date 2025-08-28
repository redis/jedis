package redis.clients.jedis.builders;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.net.URI;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.providers.PooledConnectionProvider;

/**
 * Reflection-based coverage test that iterates over all public JedisPooled constructors and
 * validates that each constructor parameter can be provided by either
 * DefaultJedisClientConfig.builder() or JedisPooled.builder() (directly or via a custom
 * ConnectionProvider). It reports any uncovered constructors and missing parameters.
 */
public class JedisPooledConstructorReflectionTest {

  private static final Logger log = LoggerFactory
      .getLogger(JedisPooledConstructorReflectionTest.class);

  @Test
  @DisplayName("Builder coverage of JedisPooled constructors (reflection)")
  void testConstructorParameterCoverageReport() {
    Constructor<?>[] ctors = JedisPooled.class.getConstructors();
    int total = 0, covered = 0;

    StringBuilder uncoveredReport = new StringBuilder();

    for (Constructor<?> ctor : ctors) {
      log.info("Testing constructor: {}", ctor);
      total++;
      java.lang.reflect.Parameter[] params = ctor.getParameters();

      boolean[] paramCovered = new boolean[params.length];
      String[] paramCoverageBy = new String[params.length];
      String[] paramWhyMissing = new String[params.length];

      // Pass 1: mark simple, unambiguous mappings by type/name
      for (int i = 0; i < params.length; i++) {
        java.lang.reflect.Parameter p = params[i];
        Class<?> t = p.getType();
        String name = safeName(p);

        if (t == HostAndPort.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().hostAndPort(HostAndPort)";
        } else if (t == URI.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().fromURI(URI)";
        } else if (t == GenericObjectPoolConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().poolConfig(...)";
        } else if (t == JedisClientConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().clientConfig(DefaultJedisClientConfig...)";
        } else if (t == SSLSocketFactory.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslSocketFactory(...)";
        } else if (t == SSLParameters.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslParameters(...)";
        } else if (t == HostnameVerifier.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().hostnameVerifier(...)";
        } else if (t == Cache.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().cache(...)";
        } else if (t == CacheConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().cacheConfig(...)";
        } else if (t == CommandExecutor.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().commandExecutor(...)";
        } else if (t == RedisProtocol.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().redisProtocol(...)";
        } else if (t == ConnectionProvider.class || t == PooledConnectionProvider.class
            || t == PooledObjectFactory.class || t == JedisSocketFactory.class) {
              // Considered covered via custom provider path
              paramCovered[i] = true;
              paramCoverageBy[i] = "Custom ConnectionProvider via JedisPooled.builder().connectionProvider(...)";
            } else
          if (t == String.class) {
            String lname = name.toLowerCase();
            if (lname.contains("url") || lname.contains("uri")) {
              paramCovered[i] = true;
              paramCoverageBy[i] = "JedisPooled.builder().fromURI(String)";
            } else if (lname.contains("host")) {
              // Will associate with a port int in pass 2
              paramCoverageBy[i] = "JedisPooled.builder().hostAndPort(host,int)";
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
              // Heuristic: if any int param exists, assume this is host to pair with port
              if (hasType(params, int.class) || hasType(params, Integer.class)) {
                paramCoverageBy[i] = "JedisPooled.builder().hostAndPort(host,int)";
              } else {
                // Otherwise assume URL
                paramCovered[i] = true;
                paramCoverageBy[i] = "JedisPooled.builder().fromURI(String)";
              }
            }
          } else if (t == int.class || t == Integer.class) {
            String lname = name.toLowerCase();
            if (lname.contains("port")) {
              // Will be paired with host
              paramCoverageBy[i] = "JedisPooled.builder().hostAndPort(host,int)";
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
                // Legacy single timeout: maps to both connection and socket timeouts
                paramCovered[i] = true;
                paramCoverageBy[i] = "DefaultJedisClientConfig.builder().connectionTimeoutMillis(...)+socketTimeoutMillis(...)";
              } else {
                // Generic: map the first int after host to port if not yet paired, else treat as
                // timeout
                paramCoverageBy[i] = "DefaultJedisClientConfig.builder().connectionTimeoutMillis/socketTimeoutMillis(...)";
              }
          } else if (t == boolean.class || t == Boolean.class) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().ssl(...)";
          } else if (t == CommandObjects.class) {
            // Provided internally by builder, not directly configurable
            paramCovered[i] = true;
            paramCoverageBy[i] = "Internal (builder-provided CommandObjects)";
          } else {
            paramCovered[i] = false;
            paramWhyMissing[i] = "No known builder mapping for type: " + t.getSimpleName();
          }
      }

      // Pass 2: pair host/port if needed
      int hostIdx = findPotentialHostStringIndex(params);
      int portIdx = findPortIndex(params);
      if (hostIdx != -1 && portIdx != -1) {
        // Both are coverable via hostAndPort
        paramCovered[hostIdx] = true;
        if (paramCoverageBy[hostIdx] == null)
          paramCoverageBy[hostIdx] = "JedisPooled.builder().hostAndPort(host,int)";
        paramCovered[portIdx] = true;
        if (paramCoverageBy[portIdx] == null)
          paramCoverageBy[portIdx] = "JedisPooled.builder().hostAndPort(host,int)";
      }

      // Evaluate constructor coverage
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
    // This test reports coverage; it does not fail on gaps, but logs them clearly.
  }

  // ===== Mapping helpers for coverage test =====

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
    // fallback: choose the first int when there is also a host string present
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
    // fallback: if there is an int parameter and exactly one String among (String,int,...), treat
    // that String as host
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
