package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.GEOSHAPE;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class GeoShapeField extends SchemaField {

  public enum CoordinateSystem {

    /**
     * For cartesian (X,Y).
     */
    FLAT,

    /**
     * For geographic (lon, lat).
     */
    SPHERICAL
  }

  private final CoordinateSystem system;

  public GeoShapeField(String fieldName, CoordinateSystem system) {
    super(fieldName);
    this.system = system;
  }

  public GeoShapeField(FieldName fieldName, CoordinateSystem system) {
    super(fieldName);
    this.system = system;
  }

  public static GeoShapeField of(String fieldName, CoordinateSystem system) {
    return new GeoShapeField(fieldName, system);
  }

  @Override
  public GeoShapeField as(String attribute) {
    super.as(attribute);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName).add(GEOSHAPE).add(system);
  }
}
