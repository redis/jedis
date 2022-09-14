package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.GEO;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class GeoField extends SchemaField {

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

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(GEO);
  }
}
