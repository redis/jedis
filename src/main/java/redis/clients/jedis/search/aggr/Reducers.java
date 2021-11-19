package redis.clients.jedis.search.aggr;

import java.util.List;

/**
 * Created by mnunberg on 2/22/18.
 */
public class Reducers {

  public static Reducer count() {
    return new Reducer() {
      @Override
      public String getName() {
        return "COUNT";
      }
    };
  }

  private static Reducer singleFieldReducer(String name, String field) {
    return new Reducer(field) {
      @Override
      public String getName() {
        return name;
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
    return new Reducer(field) {
      @Override
      public String getName() {
        return "QUANTILE";
      }

      @Override
      protected List<String> getOwnArgs() {
        List<String> args = super.getOwnArgs();
        args.add(Double.toString(percentile));
        return args;
      }
    };
  }

  /**
   * REDUCE FIRST_VALUE {nargs} {property} [BY {property} [ASC|DESC]]
   *
   * @param field
   * @param sortBy
   * @return Reducer
   */
  public static Reducer first_value(String field, SortedField sortBy) {
    return new Reducer(field) {
      @Override
      public String getName() {
        return "FIRST_VALUE";
      }

      @Override
      protected List<String> getOwnArgs() {
        List<String> args = super.getOwnArgs();
        if (sortBy != null) {
          args.add("BY");
          args.add(sortBy.getField());
          args.add(sortBy.getOrder());
        }
        return args;
      }
    };
  }

  public static Reducer first_value(String field) {
    return first_value(field, null);
  }

  public static Reducer to_list(String field) {
    return singleFieldReducer("TOLIST", field);
  }

  public static Reducer random_sample(String field, int size) {
    return new Reducer(field) {
      @Override
      public String getName() {
        return "RANDOM_SAMPLE";
      }

      @Override
      protected List<String> getOwnArgs() {
        List<String> args = super.getOwnArgs();
        args.add(Integer.toString(size));
        return args;
      }
    };
  }
}
