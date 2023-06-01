package redis.clients.jedis.search.aggr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
}
