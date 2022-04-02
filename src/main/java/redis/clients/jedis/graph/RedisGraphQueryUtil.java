package redis.clients.jedis.graph;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisGraphQueryUtil {

  public static final List<String> DUMMY_LIST = Collections.emptyList();
  public static final Map<String, List<String>> DUMMY_MAP = Collections.emptyMap();
  public static final String COMPACT_STRING = "--COMPACT";
  public static final String TIMEOUT_STRING = "TIMEOUT";

  private RedisGraphQueryUtil() {
  }

  /**
   * Prepare and formats a query and query arguments
   *
   * @param query - query
   * @param params - query parameters
   * @return query with parameters header
   */
  public static String prepareQuery(String query, Map<String, Object> params) {
    StringBuilder sb = new StringBuilder("CYPHER ");
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      sb
          .append(entry.getKey())
          .append('=')
          .append(valueToString(entry.getValue()))
          .append(' ');
    }
    sb.append(query);
    return sb.toString();
  }

  private static String valueToString(Object value) {
    if (value == null) {
      return "null";
    }

    if (value instanceof String) {
      return quoteString((String) value);
    }
    if (value instanceof Character) {
      return quoteString(((Character) value).toString());
    }

    if (value instanceof Object[]) {
      return arrayToString((Object[]) value);
    }
    if (value instanceof List) {
      return arrayToString((List<Object>) value);
    }
    return value.toString();
  }

  private static String quoteString(String str) {
    StringBuilder sb = new StringBuilder(str.length() + 12);
    sb.append('"');
    sb.append(str.replace("\"", "\\\""));
    sb.append('"');
    return sb.toString();
  }

  private static String arrayToString(Object[] arr) {
//    StringBuilder sb = new StringBuilder().append('[');
//    sb.append(String.join(", ", Arrays.stream(arr).map(RedisGraphQueryUtil::valueToString).collect(Collectors.toList())));
//    sb.append(']');
//    return sb.toString();
    return arrayToString(Arrays.asList(arr));
  }

  private static String arrayToString(List<Object> arr) {
    StringBuilder sb = new StringBuilder().append('[');
    sb.append(String.join(", ", arr.stream().map(RedisGraphQueryUtil::valueToString).collect(Collectors.toList())));
    sb.append(']');
    return sb.toString();
  }

//  public static String prepareProcedure(String procedure, List<String> args, Map<String, List<String>> kwargs) {
//    args = args.stream().map(RedisGraphQueryUtil::quoteString).collect(Collectors.toList());
//    StringBuilder queryStringBuilder = new StringBuilder();
//    queryStringBuilder.append("CALL ").append(procedure).append('(');
//    int i = 0;
//    for (; i < args.size() - 1; i++) {
//      queryStringBuilder.append(args.get(i)).append(',');
//    }
//    if (i == args.size() - 1) {
//      queryStringBuilder.append(args.get(i));
//    }
//    queryStringBuilder.append(')');
//    List<String> kwargsList = kwargs.getOrDefault("y", null);
//    if (kwargsList != null) {
//      i = 0;
//      for (; i < kwargsList.size() - 1; i++) {
//        queryStringBuilder.append(kwargsList.get(i)).append(',');
//
//      }
//      queryStringBuilder.append(kwargsList.get(i));
//    }
//    return queryStringBuilder.toString();
//  }
}
