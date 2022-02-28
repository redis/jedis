package redis.clients.jedis.json;

import java.util.Objects;

/**
 * an RFC6901 implementation that convert json pointer path to json path.
 */
class JsonPointer {
  
  static String parse(String path, String rootPath) {
    Objects.requireNonNull(path, "Json Pointer Path cannot be null.");
    
    if (path.isEmpty()) {
      // "" means all document 
      return rootPath;
    }
    if (path.charAt(0) != '/') {
      throw new IllegalArgumentException("Json Pointer Path must start with '/'.");
    }
    
    final char[] ary = path.toCharArray();
    StringBuilder result = new StringBuilder();
    StringBuilder builder = new StringBuilder();
    boolean number = true;
    char prev = '/';
    for (int i = 1; i < ary.length; i++) {
      char c = ary[i];
      switch (c) {
      case '~':
        if (prev == '~') {
          number = false;
          builder.append('~');
        }
        break;
      case '/':
        if (prev == '~') {
          number = false;
          builder.append('~');
        }
        if (builder.length() > 0 && number) {
          result.append(".[").append(builder).append("]");
        } else {
          result.append(".[\"").append(builder).append("\"]");
        }
        number = true;
        builder.setLength(0);
        break;
      case '0':
        if (prev == '~') {
          number = false;
          builder.append("~");
        } else {
          builder.append(c);
        }
        break;
      case '1':
        if (prev == '~') {
          number = false;
          builder.append("/");
        } else {
          builder.append(c);
        }
        break;
      default:
        if (prev == '~') {
          number = false;
          builder.append('~');
        }
        if (c < '0' || c > '9') {
          number = false;
        }
        builder.append(c);
        break;
      }
      prev = c;
    }
    if (prev == '~') {
      number = false;
      builder.append("~");
    }
    if (builder.length() > 0) {
      if (number) {
        result.append(".[").append(builder).append("]");
      } else {
        result.append(".[\"").append(builder).append("\"]");
      }
    } else if (prev == '/') {
      result.append(".[\"").append(builder).append("\"]");
    }
    return result.toString();
  }
}
