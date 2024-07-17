package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class NumericField extends SchemaField {

  private boolean indexMissing;
  private boolean sortable;
  private boolean noIndex;

  public NumericField(String fieldName) {
    super(fieldName);
  }

  public NumericField(FieldName fieldName) {
    super(fieldName);
  }

  public static NumericField of(String fieldName) {
    return new NumericField(fieldName);
  }

  public static NumericField of(FieldName fieldName) {
    return new NumericField(fieldName);
  }

  @Override
  public NumericField as(String attribute) {
    super.as(attribute);
    return this;
  }

  public NumericField indexMissing() {
    this.indexMissing = true;
    return this;
  }

  /**
   * Sorts the results by the value of this field.
   */
  public NumericField sortable() {
    this.sortable = true;
    return this;
  }

  /**
   * Avoid indexing.
   */
  public NumericField noIndex() {
    this.noIndex = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(NUMERIC);

    if (indexMissing) {
      args.add(INDEXMISSING);
    }

    if (sortable) {
      args.add(SORTABLE);
    }

    if (noIndex) {
      args.add(NOINDEX);
    }
  }
}
