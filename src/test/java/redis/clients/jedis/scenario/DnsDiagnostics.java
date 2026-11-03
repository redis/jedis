package redis.clients.jedis.scenario;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Properties;

import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Follows an endpoint's DNS resolution on a daemon thread, sampling once per second from two views:
 * the JVM's {@link InetAddress} (subject to the JVM cache) and a direct query to the system
 * resolver (JNDI, JVM cache bypassed). A diverging pair of samples is direct evidence of stale
 * client-side resolution around an endpoint rebind.
 * <p>
 * Messages log at TRACE and {@link #follow} is a no-op unless TRACE is enabled for this class —
 * enable it in the logging configuration to trigger the diagnostics.
 */
final class DnsDiagnostics implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(DnsDiagnostics.class);
  private static final DnsDiagnostics DISABLED = new DnsDiagnostics(null);

  private final Thread sampler;

  private DnsDiagnostics(Thread sampler) {
    this.sampler = sampler;
  }

  /** Starts following {@code host}; no-op unless TRACE is enabled for this logger. */
  static DnsDiagnostics follow(String host) {
    if (!logger.isTraceEnabled()) {
      return DISABLED;
    }
    Thread thread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        sample(host);
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }, "dns-diag");
    thread.setDaemon(true);
    thread.start();
    return new DnsDiagnostics(thread);
  }

  private static void sample(String host) {
    try {
      logger.trace("jvm {} -> {}", host, Arrays.toString(InetAddress.getAllByName(host)));
    } catch (Exception e) {
      logger.trace("jvm {} -> {}", host, e.toString());
    }
    try {
      Properties env = new Properties();
      env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
      logger.trace("dns {} -> {}", host,
        new InitialDirContext(env).getAttributes(host, new String[] { "A" }).get("A"));
    } catch (Exception e) {
      logger.trace("dns {} -> {}", host, e.toString());
    }
  }

  @Override
  public void close() {
    if (sampler != null) {
      sampler.interrupt();
    }
  }
}
