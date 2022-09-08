package redis.clients.jedis.search.querybuilder;

import java.util.StringJoiner;

/**
 * Created by mnunberg on 2/23/18.
 */
public class ValueNode implements Node {

  private final Value[] values;
  private final String field;
  private final String joinString;

  public ValueNode(String field, String joinstr, Value... values) {
    this.field = field;
    this.values = values;
    this.joinString = joinstr;
  }

  private static Value[] fromStrings(String[] values) {
    Value[] objs = new Value[values.length];
    for (int i = 0; i < values.length; i++) {
      objs[i] = Values.value(values[i]);
    }
    return objs;
  }

  public ValueNode(String field, String joinstr, String... values) {
    this(field, joinstr, fromStrings(values));
  }

  private String formatField() {
    if (field == null || field.isEmpty()) {
      return "";
    }
    return '@' + field + ':';
  }

  private String toStringCombinable(Parenthesize mode) {
    StringBuilder sb = new StringBuilder(formatField());
    if (values.length > 1 || mode == Parenthesize.ALWAYS) {
      sb.append('(');
    }
    StringJoiner sj = new StringJoiner(joinString);
    for (Value v : values) {
      sj.add(v.toString());
    }
    sb.append(sj.toString());
    if (values.length > 1 || mode == Parenthesize.ALWAYS) {
      sb.append(')');
    }
    return sb.toString();
  }

  private String toStringDefault(Parenthesize mode) {
    boolean useParen = mode == Parenthesize.ALWAYS;
    if (!useParen) {
      useParen = mode != Parenthesize.NEVER && values.length > 1;
    }
    StringBuilder sb = new StringBuilder();
    if (useParen) {
      sb.append('(');
    }
    StringJoiner sj = new StringJoiner(joinString);
    for (Value v : values) {
      sj.add(formatField() + v.toString());
    }
    sb.append(sj.toString());
    if (useParen) {
      sb.append(')');
    }
    return sb.toString();
  }

  @Override
  public String toString(Parenthesize mode) {
    if (values[0].isCombinable()) {
      return toStringCombinable(mode);
    }
    return toStringDefault(mode);
  }
}
