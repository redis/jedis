package redis.clients.jedis.search.schemafields;

import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.FieldName;

public abstract class SchemaField implements IParams {

  protected final FieldName fieldName;

  public SchemaField(String fieldName) {
    this.fieldName = new FieldName(fieldName);
  }

  public SchemaField(FieldName fieldName) {
    this.fieldName = fieldName;
  }

  public SchemaField as(String attribute) {
    fieldName.as(attribute);
    return this;
  }
}
