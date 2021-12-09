package redis.clients.jedis.tairhash;

public class ExhgetWithVerResult<T> {
  private T value;
  private long ver;

  public ExhgetWithVerResult(T value, long ver) {
    this.value = value;
    this.ver = ver;
  }

  public T getValue() {
    return value;
  }

  public long getVer() {
    return ver;
  }

  public void setValue(T value) {
    this.value = value;
  }

  public void setVer(long ver) {
    this.ver = ver;
  }
}
