package redis.clients.jedis.search;

import java.util.List;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

import redis.clients.jedis.util.SafeEncoder;

public class FieldName implements IParams {

  private static final String AS_ENCODED = "AS";
  private static final byte[] AS_BINARY = SafeEncoder.encode(AS_ENCODED);
  private static final byte[] AS = SafeEncoder.encode("AS");

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

  public int addCommandEncodedArguments(List<String> args) {
    args.add(name);
    if (attribute == null) {
      return 1;
    }

    args.add(AS_ENCODED);
    args.add(attribute);
    return 3;
  }

  public int addCommandBinaryArguments(List<byte[]> args) {
    args.add(SafeEncoder.encode(name));
    if (attribute == null) {
      return 1;
    }

    args.add(AS_BINARY);
    args.add(SafeEncoder.encode(attribute));
    return 3;
  }

  public int addCommandArguments(CommandArguments args) {
    args.add(SafeEncoder.encode(name));
    if (attribute == null) {
      return 1;
    }

    args.add(AS);
    args.add(SafeEncoder.encode(attribute));
    return 3;
  }

  @Override
  public void addParams(CommandArguments args) {
    addCommandArguments(args);
  }

  @Deprecated // TODO: remove?
  String getName() {
    return name;
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
