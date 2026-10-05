package io.redis.examples;

import java.util.Map;

import redis.clients.jedis.RedisClient;
import redis.clients.jedis.search.FTProfileParams;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.ProfilingInfo;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;

/**
 * TEMPORARY troubleshooting helper, DO NOT MERGE. Runs the example queries through FT.PROFILE and
 * prints every profile reply in place, marked PASS or MISS against the expected result size.
 */
final class ReproProfile {

    private ReproProfile() {
    }

    static SearchResult search(RedisClient jedis, String idx, String query, FTSearchParams params,
            long expectedTotal) {
        Map.Entry<SearchResult, ProfilingInfo> e = jedis.ftProfileSearch(idx,
            FTProfileParams.profileParams(), query,
            params == null ? FTSearchParams.searchParams() : params);
        print("FT.PROFILE SEARCH", idx, query, e.getKey().getTotalResults(), expectedTotal, e.getValue());
        return e.getKey();
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
        System.out.println("#### PROFILE " + (got == expected ? "PASS" : "MISS") + " time_ms="
            + System.currentTimeMillis() + " " + cmd + " idx=" + idx + " q='" + query + "' got=" + got
            + " expected=" + expected + "\n" + info.getProfilingInfo());
    }
}
