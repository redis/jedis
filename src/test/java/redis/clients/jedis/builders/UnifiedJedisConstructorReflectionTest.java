package redis.clients.jedis.builders;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.net.URI;
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
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Reflection-based coverage test for UnifiedJedis constructors against builder API. Since
 * UnifiedJedis is the base, we treat many low-level components as covered via custom
 * ConnectionProvider or builder-provided internals.
 */
@EnabledIfSystemProperty(named = "with-param-names", matches = "true")
public class UnifiedJedisConstructorReflectionTest {

  private static final Logger log = LoggerFactory
      .getLogger(UnifiedJedisConstructorReflectionTest.class);

  @Test
  @DisplayName("Builder coverage of UnifiedJedis constructors (reflection)")
  void testConstructorParameterCoverageReport() {
    Constructor<?>[] ctors = UnifiedJedis.class.getConstructors();
    int total = 0, covered = 0;

    StringBuilder uncoveredReport = new StringBuilder();

    for (Constructor<?> ctor : ctors) {
      if (isUnsafeConstructor(ctor) || clusterConstructorThatShouldBeDeprecatedAndRemoved(ctor)
          || retriesConstructorThatShouldBeIncorporatedIntoBuilderAsDefault(ctor)
          || multiClusterPooledConnectionProviderShouldBeReplacedWithResilientClient(ctor)) {
        // Exclude unsafe constructors from analysis as requested
        continue;
      }
      total++;
      java.lang.reflect.Parameter[] params = ctor.getParameters();

      boolean[] paramCovered = new boolean[params.length];
      String[] paramCoverageBy = new String[params.length];
      String[] paramWhyMissing = new String[params.length];

      for (int i = 0; i < params.length; i++) {
        java.lang.reflect.Parameter p = params[i];
        Class<?> t = p.getType();
        String name = safeName(p);

        if (t == HostAndPort.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "JedisPooled.builder().hostAndPort(HostAndPort)";
        } else if (t == URI.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "builder().fromURI(URI)";
        } else if (t == ConnectionProvider.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "Custom ConnectionProvider via builder.connectionProvider(...)";
        } else if (t == GenericObjectPoolConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "builder.poolConfig(...) (in concrete builders)";
        } else if (t == JedisClientConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "builder.clientConfig(DefaultJedisClientConfig...)";
        } else if (t == Cache.class || t == CacheConfig.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = t == Cache.class ? "builder.cache(...)" : "builder.cacheConfig(...)";
        } else if (t == CommandExecutor.class || t == CommandObjects.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = t == CommandExecutor.class ? "builder.commandExecutor(...)"
              : "Internal (builder-provided CommandObjects)";
        } else if (t == RedisProtocol.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "builder.redisProtocol(...)";
        } else if (t == SSLSocketFactory.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslSocketFactory(...)";
        } else if (t == SSLParameters.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().sslParameters(...)";
        } else if (t == HostnameVerifier.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().hostnameVerifier(...)";
        } else if (t == String.class) {
          String lname = name.toLowerCase();
          if (lname.contains("url") || lname.contains("uri")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "JedisPooled.builder().fromURI(String)";
          } else {
            paramCovered[i] = false;
            paramWhyMissing[i] = "No known builder mapping for type: " + t.getSimpleName();
          }
        } else if (t == int.class || t == Integer.class) {
          String lname = name.toLowerCase();
          if (lname.contains("timeout")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().connectionTimeoutMillis/socketTimeoutMillis/blockingSocketTimeoutMillis(...)";
          } else if (lname.contains("attempt")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "Cluster/Sentinel builders manage attempts";
          } else if (lname.contains("db") || lname.contains("database")) {
            paramCovered[i] = true;
            paramCoverageBy[i] = "DefaultJedisClientConfig.builder().database(...)";
          }
        } else if (t == boolean.class || t == Boolean.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "DefaultJedisClientConfig.builder().ssl(...)";
        } else if (t == PooledObjectFactory.class) {
          paramCovered[i] = true;
          paramCoverageBy[i] = "Custom provider via PooledObjectFactory in concrete builders";
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

  private static boolean isUnsafeConstructor(Constructor<?> ctor) {
    Class<?>[] types = ctor.getParameterTypes();
    for (Class<?> t : types) {
      if (t == Connection.class || t == JedisSocketFactory.class
          || t.getSimpleName().equals("ShardedConnectionProvider")) {
        return true;
      }
    }
    return false;
  }

  // FIXME: Remove this when we use command executor with retries by default
  private static boolean retriesConstructorThatShouldBeIncorporatedIntoBuilderAsDefault(
      Constructor<?> ctor) {
    return ctor.toString().equals(
      "public redis.clients.jedis.UnifiedJedis(redis.clients.jedis.providers.ConnectionProvider,int,java.time.Duration)");
  }

  // FIXME: Remove this when we remove the deprecated Cluster-related constructors from UnifiedJedis
  private static boolean clusterConstructorThatShouldBeDeprecatedAndRemoved(Constructor<?> ctor) {
    Class<?>[] types = ctor.getParameterTypes();
    return types.length > 1 && ((types[0] == Set.class && types[1] == JedisClientConfig.class)
        || types[0].getSimpleName().equals("ClusterConnectionProvider"));
  }

  // FIXME: Remove this when we add convince class and builder for ResilientClient
  private static boolean multiClusterPooledConnectionProviderShouldBeReplacedWithResilientClient(
      Constructor<?> ctor) {
    Class<?>[] types = ctor.getParameterTypes();
    return types.length == 1 && types[0].getSimpleName().equals("MultiDbConnectionProvider");
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
