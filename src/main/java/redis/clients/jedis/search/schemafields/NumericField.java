package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class NumericField extends SchemaField {

  private boolean isMissing;
  private boolean isEmpty;
  private boolean isNull;
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

  public NumericField isMissing() {
    this.isMissing = true;
    return this;
  }

  public NumericField isEmpty() {
    this.isEmpty = true;
    return this;
  }

  public NumericField isNull() {
    this.isNull = true;
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

    if (isMissing) args.add(ISMISSING);
    if (isEmpty) args.add(ISEMPTY);
    if (isNull) args.add(ISNULL);

    if (sortable) {
      args.add(SORTABLE);
    }

    if (noIndex) {
      args.add(NOINDEX);
    }
  }
}
