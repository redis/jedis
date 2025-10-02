package redis.clients.jedis.json;

/**
 * Path is a RedisJSON (v1) path, representing a valid path into an object.
 * @deprecated RedisJSON (v1) support is deprecated.
 */
@Deprecated
public class Path {

  public static final Path ROOT_PATH = new Path(".");

  private final String strPath;

  public Path(final String strPath) {
    this.strPath = strPath;
  }

  @Override
  public String toString() {
    return strPath;
  }

  public static Path of(final String strPath) {
    return new Path(strPath);
  }
  
  public static Path ofJsonPointer(final String strPath) {
    return new Path(JsonPointer.parse(strPath, ROOT_PATH.strPath));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Path)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return this.toString().equals(((Path) obj).toString());
  }

  @Override
  public int hashCode() {
    return strPath.hashCode();
  }
}
