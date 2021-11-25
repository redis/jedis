package redis.clients.jedis.search.aggr;

/**
 * Created by mnunberg on 2/22/18.
 */
public class SortedField {

  public enum SortOrder {
    ASC, DESC
  }

  private final String fieldName;
  private final SortOrder sortOrder;

  public SortedField(String fieldName, SortOrder order) {
    this.fieldName = fieldName;
    this.sortOrder = order;
  }

  public final String getOrder() {
    return sortOrder.toString();
  }

  public final String getField() {
    return fieldName;
  }

  public static SortedField asc(String field) {
    return new SortedField(field, SortOrder.ASC);
  }

  public static SortedField desc(String field) {
    return new SortedField(field, SortOrder.DESC);
  }
}
