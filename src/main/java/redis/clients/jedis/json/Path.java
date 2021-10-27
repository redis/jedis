package redis.clients.jedis.json;

import java.util.Objects;

/**
 * Path is a ReJSON path, representing a valid path into an object
 */
public class Path {

  public static final Path ROOT_PATH = new Path(".");

  private final String path;

  public Path(final String path) {
    this.path = path;
  }

  @Override
  public String toString() {
    return path;
  }

  public static Path of(final String path) {
    return new Path(path);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof Path)) return false;
    return Objects.equals(path, ((Path) obj).path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }
}
