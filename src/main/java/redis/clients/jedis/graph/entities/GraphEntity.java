package redis.clients.jedis.graph.entities;

import java.util.*;

/**
 * This is an abstract class for representing a graph entity. A graph entity has an id and a set of
 * properties. The properties are mapped and accessed by their names.
 */
public abstract class GraphEntity {

  protected long id;
  protected final Map<String, Property<?>> propertyMap;

  public GraphEntity() {
      propertyMap = new HashMap<>();
  }

  /**
   * Use this constructor to reduce memory allocations 
   * when properties are added to the edge
   * @param propertiesCapacity preallocate the capacity for the properties
   */
  public GraphEntity(int propertiesCapacity) {
      propertyMap = new HashMap<>(propertiesCapacity);
  }

  /**
   * @return entity id
   */
  public long getId() {
    return id;
  }

  /**
   * @param id - entity id to be set
   */
  public void setId(long id) {
    this.id = id;
  }

  /**
   * Adds a property to the entity, by composing name, type and value to a property object
   *
   * @param name
   * @param value
   */
  public void addProperty(String name, Object value) {
    addProperty(new Property(name, value));
  }

  /**
   * @return Entity's property names, as a Set
   */
  public Set<String> getEntityPropertyNames() {
    return propertyMap.keySet();
  }

  /**
   * Add a property to the entity
   *
   * @param property
   */
  public void addProperty(Property property) {
    propertyMap.put(property.getName(), property);
  }

  /**
   * @return number of properties
   */
  public int getNumberOfProperties() {
    return propertyMap.size();
  }

  /**
   * @param propertyName - property name as lookup key (String)
   * @return property object, or null if key is not found
   */
  public Property getProperty(String propertyName) {
    return propertyMap.get(propertyName);
  }

  /**
   * @param name - the name of the property to be removed
   */
  public void removeProperty(String name) {
    propertyMap.remove(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GraphEntity)) {
      return false;
    }
    GraphEntity that = (GraphEntity) o;
    return id == that.id
        && Objects.equals(propertyMap, that.propertyMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, propertyMap);
  }

  @Override
  public abstract String toString();
}
