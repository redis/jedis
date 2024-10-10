package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class GeoField extends SchemaField {

  private boolean indexMissing;
  private boolean sortable;
  private boolean noIndex;

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

  public GeoField indexMissing() {
    this.indexMissing = true;
    return this;
  }

  public GeoField sortable() {
    this.sortable = true;
    return this;
  }

  public GeoField noIndex() {
    this.noIndex = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(GEO);

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
