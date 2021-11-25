package redis.clients.jedis.json;

/**
 * Path is a ReJSON path, representing a valid path into an object
 */
public class Path2 {

  public static final Path2 ROOT_PATH = new Path2("$");

  private final String str;

  public Path2(final String str) {
    if (str == null) {
      throw new NullPointerException("Path cannot be null.");
    }
    if (str.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be empty.");
    }
    if (str.charAt(0) == '$') {
      this.str = str;
    } else if (str.charAt(0) == '.') {
      this.str = '$' + str;
    } else {
      this.str = "$." + str;
    }
  }

  @Override
  public String toString() {
    return str;
  }

  public static Path2 of(final String path) {
    return new Path2(path);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Path2)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return this.toString().equals(((Path2) obj).toString());
  }

  @Override
  public int hashCode() {
    return str.hashCode();
  }
}
