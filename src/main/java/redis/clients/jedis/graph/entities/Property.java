package redis.clients.jedis.graph.entities;

import java.util.Objects;

/**
 * A Graph entity property. Has a name, type, and value
 *
 * @param <T>
 */
public class Property<T> {

  private final String name;
  private final T value;

  public Property(String name, T value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public T getValue() {
    return value;
  }

  private boolean valueEquals(Object value1, Object value2) {
    if (value1 instanceof Integer) value1 = ((Integer) value1).longValue();
    if (value2 instanceof Integer) value2 = ((Integer) value2).longValue();
    return Objects.equals(value1, value2);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Property)) return false;
    Property<?> property = (Property<?>) o;
    return Objects.equals(name, property.name)
        && valueEquals(value, property.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  /**
   * Default toString implementation
   *
   * @return
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Property{");
    sb.append("name='").append(name).append('\'');
    sb.append(", value=").append(value);
    sb.append('}');
    return sb.toString();
  }
}
