package redis.clients.jedis.scenario;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The few facts about a Redis Enterprise database that `rladmin status` can answer and the client
 * cannot: the database name, its endpoint uid, and the node that endpoint is currently bound to.
 * <p>
 * The endpoint uid and node uid are what `rladmin bind endpoint &lt;uid&gt; exclude &lt;node&gt;`
 * requires. The REST API cannot substitute here: `/v1/bdbs` exposes the endpoint uid but not which
 * node the endpoint is bound to - that lives in the CCS `proxy_uids` - and inferring it from the
 * endpoint address only holds for `proxy_policy single`.
 */
final class RedisEnterpriseClusterStatus {

  /**
   * The DATABASES section has a dynamic column set - rladmin inserts a MODULE column whenever any
   * database in the cluster loads modules, and appends a traffic column conditionally - so anchor
   * only on DB:ID and NAME, which are always the first two.
   */
  private static final Pattern DATABASE = Pattern.compile("^db:(\\d+)\\s+(\\S+).*");

  private static final Pattern ENDPOINT = Pattern
      .compile("^db:(\\d+)\\s+\\S+\\s+endpoint:(\\d+:\\d+)\\s+node:(\\d+)\\s+.*");

  private final String dbName;

  private final String endpointUid;

  private final String boundNodeUid;

  private final String rawStatus;

  private RedisEnterpriseClusterStatus(String dbName, String endpointUid, String boundNodeUid,
      String rawStatus) {
    this.dbName = dbName;
    this.endpointUid = endpointUid;
    this.boundNodeUid = boundNodeUid;
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
    String boundNodeUid = null;

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
          if (matcher.matches() && bdbId.equals(matcher.group(1)) && endpointUid == null) {
            endpointUid = matcher.group(2);
            boundNodeUid = matcher.group(3);
          }
        }
      }
    }

    if (dbName == null || endpointUid == null) {
      // A hard failure, not a skip: the caller only gets here once the discovery service has
      // already answered, so the cluster is up and rladmin's output format must have drifted.
      throw new IllegalStateException("Could not parse the name and endpoint of bdb " + bdbId
          + " from rladmin status. Raw output:\n" + status);
    }

    return new RedisEnterpriseClusterStatus(dbName, endpointUid, boundNodeUid, status);
  }

  /** Database name, which is also the Sentinel master name the discovery service announces. */
  String getDbName() {
    return dbName;
  }

  /** e.g. {@code 2:1} - what {@code rladmin bind endpoint <uid>} expects. */
  String getEndpointUid() {
    return endpointUid;
  }

  /** Bare node uid, e.g. {@code 3} - what {@code exclude}/{@code include} expect. */
  String getBoundNodeUid() {
    return boundNodeUid;
  }

  String getRawStatus() {
    return rawStatus;
  }

  @Override
  public String toString() {
    return "db:" + dbName + " endpoint:" + endpointUid + " on node:" + boundNodeUid;
  }
}
