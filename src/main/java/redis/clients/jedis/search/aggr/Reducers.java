package redis.clients.jedis.search.aggr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import redis.clients.jedis.annots.Experimental;

/**
 * Created by mnunberg on 2/22/18.
 */
public class Reducers {

  public static Reducer count() {
    return new Reducer("COUNT") {
      @Override protected List<Object> getOwnArgs() {
        return Collections.emptyList();
      }
    };
  }

  private static Reducer singleFieldReducer(String name, String field) {
    return new Reducer(name, field) {
      @Override protected List<Object> getOwnArgs() {
        return Collections.emptyList();
      }
    };
  }

  public static Reducer count_distinct(String field) {
    return singleFieldReducer("COUNT_DISTINCT", field);
  }

  public static Reducer count_distinctish(String field) {
    return singleFieldReducer("COUNT_DISTINCTISH", field);
  }

  public static Reducer sum(String field) {
    return singleFieldReducer("SUM", field);
  }

  public static Reducer min(String field) {
    return singleFieldReducer("MIN", field);
  }

  public static Reducer max(String field) {
    return singleFieldReducer("MAX", field);
  }

  public static Reducer avg(String field) {
    return singleFieldReducer("AVG", field);
  }

  public static Reducer stddev(String field) {
    return singleFieldReducer("STDDEV", field);
  }

  public static Reducer quantile(String field, double percentile) {
    return new Reducer("QUANTILE", field) {
      @Override protected List<Object> getOwnArgs() {
        return Arrays.asList(percentile);
      }
    };
  }

  public static Reducer first_value(String field) {
    return singleFieldReducer("FIRST_VALUE", field);
  }

  /**
   * REDUCE FIRST_VALUE {nargs} {property} [BY {property} [ASC|DESC]]
   *
   * @param field
   * @param sortBy
   * @return Reducer
   */
  public static Reducer first_value(String field, SortedField sortBy) {
    return new Reducer("FIRST_VALUE", field) {
      @Override protected List<Object> getOwnArgs() {
        return Arrays.asList("BY", sortBy.getField(), sortBy.getOrder());
      }
    };
  }

  public static Reducer to_list(String field) {
    return singleFieldReducer("TOLIST", field);
  }

  public static Reducer random_sample(String field, int size) {
    return new Reducer("RANDOM_SAMPLE", field) {
      @Override protected List<Object> getOwnArgs() {
        return Arrays.asList(size);
      }
    };
  }

  /**
   * REDUCE COLLECT — gather per-document projections within a GROUPBY group.
   * <p>
   * Returns a {@link CollectReducer} builder; configure projected fields via
   * {@link CollectReducer#fields(String...) fields(...)} or
   * {@link CollectReducer#fieldsAll() fieldsAll()}, then optionally chain
   * {@link CollectReducer#sortBy(SortedField...) sortBy(...)} and
   * {@link CollectReducer#limit(int, int) limit(...)} before calling {@link Reducer#as(String)}.
   * <p>
   * <b>Experimental.</b> Both the underlying Redis Search feature and this API may change.
   * Before issuing COLLECT queries the server must be configured with
   * {@code CONFIG SET search-enable-unstable-features yes}; otherwise the server returns
   * {@code SEARCH_QUERY_BAD `COLLECT` is unavailable when `ENABLE_UNSTABLE_FEATURES` is off}.
   *
   * @see CollectReducer
   */
  @Experimental
  public static CollectReducer collect() {
    return new CollectReducer();
  }
}
