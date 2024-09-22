package redis.clients.jedis.search;

import java.util.List;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

public class FieldName implements IParams {

  private final String name;
  private String attribute;

  public FieldName(String name) {
    this.name = name;
  }

  public FieldName(String name, String attribute) {
    this.name = name;
    this.attribute = attribute;
  }

  public FieldName as(String attribute) {
    if (attribute == null) {
      throw new IllegalArgumentException("Setting null as field attribute is not allowed.");
    }
    if (this.attribute != null) {
      throw new IllegalStateException("Attribute for this field is already set.");
    }
    this.attribute = attribute;
    return this;
  }

  public final String getName() {
    return name;
  }

  public final String getAttribute() {
    return attribute;
  }

  public int addCommandArguments(List<Object> args) {
    args.add(name);
    if (attribute == null) {
      return 1;
    }

    args.add(SearchKeyword.AS);
    args.add(attribute);
    return 3;
  }

  public int addCommandArguments(CommandArguments args) {
    args.add(name);
    if (attribute == null) {
      return 1;
    }

    args.add(SearchKeyword.AS);
    args.add(attribute);
    return 3;
  }

  @Override
  public void addParams(CommandArguments args) {
    addCommandArguments(args);
  }

  @Override
  public String toString() {
    return attribute == null ? name : (name + " AS " + attribute);
  }

  public static FieldName of(String name) {
    return new FieldName(name);
  }

  public static FieldName[] convert(String... names) {
    if (names == null) {
      return null;
    }
    FieldName[] fields = new FieldName[names.length];
    for (int i = 0; i < names.length; i++) {
      fields[i] = FieldName.of(names[i]);
    }
    return fields;
  }
}
