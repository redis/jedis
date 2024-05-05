package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class GeoField extends SchemaField {

  private boolean isMissing;
  private boolean isEmpty;
  private boolean isNull;

  public GeoField(String fieldName) {
    super(fieldName);
  }

  public GeoField(FieldName fieldName) {
    super(fieldName);
  }

  public static GeoField of(String fieldName) {
    return new GeoField(fieldName);
  }

  public static GeoField of(FieldName fieldName) {
    return new GeoField(fieldName);
  }

  @Override
  public GeoField as(String attribute) {
    super.as(attribute);
    return this;
  }

  public GeoField isMissing() {
    this.isMissing = true;
    return this;
  }

  public GeoField isEmpty() {
    this.isEmpty = true;
    return this;
  }

  public GeoField isNull() {
    this.isNull = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(GEO);

    if (isMissing) args.add(ISMISSING);
    if (isEmpty) args.add(ISEMPTY);
    if (isNull) args.add(ISNULL);
  }
}
