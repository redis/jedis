package io.redis.examples;

import java.util.Map;

import redis.clients.jedis.RedisClient;
import redis.clients.jedis.search.FTProfileParams;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.ProfilingInfo;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;

/**
 * TEMPORARY troubleshooting helper, DO NOT MERGE. Runs the example queries through FT.PROFILE and
 * prints every profile reply in place. Marked PASS or MISS when the expected result size is known,
 * INFO otherwise.
 */
final class ReproProfile {

  private static final long UNKNOWN = -1;

  private ReproProfile() {
  }

  static SearchResult search(RedisClient jedis, String idx, String query) {
    return search(jedis, idx, query, null, UNKNOWN);
  }

  static SearchResult search(RedisClient jedis, String idx, String query, FTSearchParams params) {
    return search(jedis, idx, query, params, UNKNOWN);
  }

  static SearchResult search(RedisClient jedis, String idx, String query, FTSearchParams params,
      long expectedTotal) {
    Map.Entry<SearchResult, ProfilingInfo> e = jedis.ftProfileSearch(idx,
      FTProfileParams.profileParams(), query,
      params == null ? FTSearchParams.searchParams() : params);
    print("FT.PROFILE SEARCH", idx, query, e.getKey().getTotalResults(), expectedTotal,
      e.getValue());
    return e.getKey();
  }

  static SearchResult search(RedisClient jedis, String idx, Query query) {
    Map.Entry<SearchResult, ProfilingInfo> e = jedis.ftProfileSearch(idx,
      FTProfileParams.profileParams(), query);
    print("FT.PROFILE SEARCH", idx, "(Query object)", e.getKey().getTotalResults(), UNKNOWN,
      e.getValue());
    return e.getKey();
  }

  static AggregationResult aggregate(RedisClient jedis, String idx, AggregationBuilder aggr) {
    return aggregate(jedis, idx, "(aggregation)", aggr, (int) UNKNOWN);
  }

  static AggregationResult aggregate(RedisClient jedis, String idx, String label,
      AggregationBuilder aggr, int expectedRows) {
    Map.Entry<AggregationResult, ProfilingInfo> e = jedis.ftProfileAggregate(idx,
      FTProfileParams.profileParams(), aggr);
    print("FT.PROFILE AGGREGATE", idx, label, e.getKey().getRows().size(), expectedRows,
      e.getValue());
    return e.getKey();
  }

  private static void print(String cmd, String idx, String query, long got, long expected,
      ProfilingInfo info) {
    String verdict = expected == UNKNOWN ? "INFO" : got == expected ? "PASS" : "MISS";
    System.out.println("#### PROFILE " + verdict + " time_ms=" + System.currentTimeMillis() + " "
        + cmd + " idx=" + idx + " q='" + query + "' got=" + got + " expected=" + expected + "\n"
        + info.getProfilingInfo());
  }
}
