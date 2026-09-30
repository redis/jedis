package redis.clients.jedis.scenario;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The few facts about a Redis Enterprise database that `rladmin status` can answer and the client
 * cannot: the database name, its endpoint uid, the nodes that proxy the endpoint, and which node
 * owns a given address.
 * <p>
 * The endpoint uid and node uid are what `rladmin bind endpoint &lt;uid&gt; exclude &lt;node&gt;`
 * requires. The REST API cannot substitute here: `/v1/bdbs` exposes the endpoint uid but not which
 * nodes proxy the endpoint - that lives in the CCS `proxy_uids`.
 * <p>
 * Note an endpoint can be proxied by several nodes at once, in which case rladmin prints one
 * ENDPOINTS row per proxy in an order that is not stable between invocations. Every proxy is
 * therefore kept rather than one arbitrary row being picked, because which node is drained decides
 * whether Redis Enterprise publishes anything at all.
 */
final class RedisEnterpriseClusterStatus {

  private static final Logger log = LoggerFactory.getLogger(RedisEnterpriseClusterStatus.class);

  /**
   * The DATABASES section has a dynamic column set - rladmin inserts a MODULE column whenever any
   * database in the cluster loads modules, and appends a traffic column conditionally - so anchor
   * only on DB:ID and NAME, which are always the first two.
   */
  private static final Pattern DATABASE = Pattern.compile("^db:(\\d+)\\s+(\\S+).*");

  private static final Pattern ENDPOINT = Pattern
      .compile("^db:(\\d+)\\s+\\S+\\s+endpoint:(\\d+:\\d+)\\s+node:(\\d+)\\s+.*");

  private static final Pattern NODE = Pattern.compile("^\\*?node:(\\d+)\\s+.*");

  /**
   * The EXTERNAL_ADDRESS column is blank on clusters without external addressing, which shifts
   * every column after it. Rather than mapping columns positionally, collect every address-shaped
   * token on the row - both the internal ADDRESS and the EXTERNAL_ADDRESS are wanted anyway.
   */
  private static final Pattern IPV4 = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

  private final String dbName;

  private final String endpointUid;

  private final List<String> proxyNodeUids;

  private final Map<String, Set<String>> nodeAddresses;

  private final String rawStatus;

  private RedisEnterpriseClusterStatus(String dbName, String endpointUid,
      List<String> proxyNodeUids, Map<String, Set<String>> nodeAddresses, String rawStatus) {
    this.dbName = dbName;
    this.endpointUid = endpointUid;
    this.proxyNodeUids = proxyNodeUids;
    this.nodeAddresses = nodeAddresses;
    this.rawStatus = rawStatus;
  }

  static RedisEnterpriseClusterStatus discover(FaultInjectionClient faultClient, String bdbId,
      Duration checkInterval, Duration timeout) throws IOException {
    return parse(
      faultClient.executeRladminCommandCapturingOutput(bdbId, "status", checkInterval, timeout),
      bdbId);
  }

  /**
   * Visible for testing so the parse can be pinned offline against captured output.
   */
  static RedisEnterpriseClusterStatus parse(String status, String bdbId) {
    if (status == null || status.trim().isEmpty()) {
      throw new IllegalStateException("rladmin status returned no output for bdb " + bdbId);
    }

    String dbName = null;
    String endpointUid = null;
    List<String> proxyNodeUids = new ArrayList<>();
    Map<String, Set<String>> nodeAddresses = new LinkedHashMap<>();

    // Sections, not the whole blob: a db:<id> row appears in DATABASES, ENDPOINTS and SHARDS, and
    // only the first two are wanted here.
    String[] sections = status.split("(?=CLUSTER NODES:|DATABASES:|ENDPOINTS:|SHARDS:)");
    for (String section : sections) {
      String trimmed = section.trim();
      if (trimmed.startsWith("DATABASES:")) {
        for (String line : trimmed.split("\\n")) {
          Matcher matcher = DATABASE.matcher(line.trim());
          if (matcher.matches() && bdbId.equals(matcher.group(1)) && dbName == null) {
            dbName = matcher.group(2);
          }
        }
      } else if (trimmed.startsWith("ENDPOINTS:")) {
        for (String line : trimmed.split("\\n")) {
          Matcher matcher = ENDPOINT.matcher(line.trim());
          if (matcher.matches() && bdbId.equals(matcher.group(1))) {
            endpointUid = matcher.group(2);
            if (!proxyNodeUids.contains(matcher.group(3))) {
              proxyNodeUids.add(matcher.group(3));
            }
          }
        }
      } else if (trimmed.startsWith("CLUSTER NODES:")) {
        for (String line : trimmed.split("\\n")) {
          Matcher matcher = NODE.matcher(line.trim());
          if (!matcher.matches()) {
            continue;
          }
          Set<String> addresses = new LinkedHashSet<>();
          Matcher address = IPV4.matcher(line);
          while (address.find()) {
            addresses.add(address.group());
          }
          nodeAddresses.put(matcher.group(1), addresses);
        }
      }
    }

    if (dbName == null || endpointUid == null) {
      // A hard failure, not a skip: the caller only gets here once the discovery service has
      // already answered, so the cluster is up and rladmin's output format must have drifted.
      throw new IllegalStateException("Could not parse the name and endpoint of bdb " + bdbId
          + " from rladmin status. Raw output:\n" + status);
    }

    return new RedisEnterpriseClusterStatus(dbName, endpointUid, proxyNodeUids, nodeAddresses,
        status);
  }

  /** Database name, which is also the Sentinel master name the discovery service announces. */
  String getDbName() {
    return dbName;
  }

  /** e.g. {@code 2:1} - what {@code rladmin bind endpoint <uid>} expects. */
  String getEndpointUid() {
    return endpointUid;
  }

  /**
   * Every node proxying the endpoint, as bare node uids such as {@code 3} - what
   * {@code exclude}/{@code include} expect - in the order rladmin printed them.
   * <p>
   * More than one entry means the endpoint is multi-proxy, so there is no single "the" node and the
   * order carries no meaning. Drain the node {@link #findNodeByAddress(String)} resolves from the
   * address the discovery service reports, or collapse the endpoint with
   * {@code bind endpoint <uid> policy single} first.
   */
  List<String> getProxyNodeUids() {
    return Collections.unmodifiableList(proxyNodeUids);
  }

  /**
   * Resolve the node that owns an address, matching against every address rladmin printed for the
   * node - both the internal {@code ADDRESS} and the {@code EXTERNAL_ADDRESS}.
   * @param address an IPv4 address, such as the one the discovery service reports as the master.
   * @return the owning bare node uid, {@code null} if no node claims the address.
   */
  String findNodeByAddress(String address) {
    if (address == null || address.trim().isEmpty()) {
      return null;
    }
    String wanted = address.trim();
    for (Map.Entry<String, Set<String>> entry : nodeAddresses.entrySet()) {
      if (entry.getValue().contains(wanted)) {
        return entry.getKey();
      }
    }
    log.warn("No node claims address {}; known node addresses are {}", wanted, nodeAddresses);
    return null;
  }

  String getRawStatus() {
    return rawStatus;
  }

  @Override
  public String toString() {
    return "db:" + dbName + " endpoint:" + endpointUid + " proxied by node:" + proxyNodeUids;
  }
}
