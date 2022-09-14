package redis.clients.jedis.search.querybuilder;

/**
 * @author mnunberg on 2/23/18.
 */
public abstract class RangeValue extends Value {

  private boolean inclusiveMin = true;
  private boolean inclusiveMax = true;

  @Override
  public boolean isCombinable() {
    return false;
  }

  protected abstract void appendFrom(StringBuilder sb, boolean inclusive);

  protected abstract void appendTo(StringBuilder sb, boolean inclusive);

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    appendFrom(sb, inclusiveMin);
    sb.append(' ');
    appendTo(sb, inclusiveMax);
    sb.append(']');
    return sb.toString();
  }

  public RangeValue inclusiveMin(boolean val) {
    inclusiveMin = val;
    return this;
  }

  public RangeValue inclusiveMax(boolean val) {
    inclusiveMax = val;
    return this;
  }
}
