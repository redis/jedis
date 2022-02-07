package redis.clients.jedis.resps;

import java.util.List;

public class LibraryInfo {

  public static class FunctionInfo {

    private final String name;
    private final String description;
    private final List<String> flags;

    public FunctionInfo(String name, String description, List<String> flags) {
      this.name = name;
      this.description = description;
      this.flags = flags;
    }

    public String getName() {
      return this.name;
    }

    public String getDescription() {
      return description;
    }

    public List<String> getFlags() {
      return flags;
    }
  }

  private String name;
  private String engine;
  private String description;
  private List<FunctionInfo> functions;
  private String code;

  public LibraryInfo(String libraryName, String engineName, String description, List<FunctionInfo> functions) {
    this.name = libraryName;
    this.engine = engineName;
    this.description = description;
    this.functions = functions;
  }

  public LibraryInfo(String libraryName, String engineName, String description, List<FunctionInfo> functions, String code) {
    this(libraryName, engineName, description, functions);
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public String getEngine() {
    return engine;
  }

  public String getDescription() {
    return description;
  }

  public List<FunctionInfo> getFunctions() {
    return functions;
  }

  public String getCode() {
    return code;
  }
}
