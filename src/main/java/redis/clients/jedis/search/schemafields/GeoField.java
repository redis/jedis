package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.GEO;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOINDEX;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class GeoField extends SchemaField {

  private boolean noIndex;

  public GeoField(String fieldName) {
    super(fieldName);
  }

  public GeoField(FieldName fieldName) {
    super(fieldName);
  }

  public static GeoField geoField(String fieldName) {
    return new GeoField(fieldName);
  }

  public static GeoField geoField(FieldName fieldName) {
    return new GeoField(fieldName);
  }

  @Override
  public GeoField as(String attribute) {
    super.as(attribute);
    return this;
  }

  /**
   * Avoid indexing.
   */
  public GeoField noIndex() {
    this.noIndex = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(GEO);

    if (noIndex) {
      args.add(NOINDEX);
    }
  }
}
