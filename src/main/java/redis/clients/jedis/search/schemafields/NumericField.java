package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOINDEX;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NUMERIC;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SORTABLE;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.UNF;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class NumericField extends SchemaField {

  private boolean sortable;
  private boolean sortableUNF;
  private boolean noIndex;

  public NumericField(String fieldName) {
    super(fieldName);
  }

  public NumericField(FieldName fieldName) {
    super(fieldName);
  }

  public static NumericField numericField(String fieldName) {
    return new NumericField(fieldName);
  }

  public static NumericField numericField(FieldName fieldName) {
    return new NumericField(fieldName);
  }

  @Override
  public NumericField as(String attribute) {
    super.as(attribute);
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
   * Sorts the results by the value of this field without normalization.
   */
  public NumericField sortableUNF() {
    this.sortableUNF = true;
    return this;
  }

  /**
   * @see TextField#sortableUNF()
   */
  public NumericField sortableUnNormalizedForm() {
    return sortableUNF();
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

    if (sortableUNF) {
      args.add(SORTABLE).add(UNF);
    } else if (sortable) {
      args.add(SORTABLE);
    }

    if (noIndex) {
      args.add(NOINDEX);
    }
  }
}
