package redis.clients.jedis.search.querybuilder;

public class LongRangeValue extends RangeValue {

  private final long from;
  private final long to;

  @Override
  public boolean isCombinable() {
    return false;
  }

  private static void appendNum(StringBuilder sb, long n, boolean inclusive) {
    if (!inclusive) {
      sb.append("(");
    }
    if (n == Long.MIN_VALUE) {
      sb.append("-inf");
    } else if (n == Long.MAX_VALUE) {
      sb.append("inf");
    } else {
      sb.append(Long.toString(n));
    }
  }

  public LongRangeValue(long from, long to) {
    this.from = from;
    this.to = to;
  }

  @Override
  protected void appendFrom(StringBuilder sb, boolean inclusive) {
    appendNum(sb, from, inclusive);
  }

  @Override
  protected void appendTo(StringBuilder sb, boolean inclusive) {
    appendNum(sb, to, inclusive);
  }
}
