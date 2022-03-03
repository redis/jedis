package redis.clients.jedis.resps;

import static redis.clients.jedis.BuilderFactory.STRING;
import static redis.clients.jedis.BuilderFactory.ENCODED_OBJECT_MAP;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;

public class LibraryInfo {

  private final String libraryName;
  private final String engine;
  private final String description;
  private final List<Map<String, Object>> functions;
  private final String libraryCode;

  public LibraryInfo(String libraryName, String engineName, String description, List<Map<String, Object>> functions) {
    this(libraryName, engineName, description, functions, null);
  }

  public LibraryInfo(String libraryName, String engineName, String description, List<Map<String, Object>> functions, String code) {
    this.libraryName = libraryName;
    this.engine = engineName;
    this.description = description;
    this.functions = functions;
    this.libraryCode = code;
  }

  public String getLibraryName() {
    return libraryName;
  }

  public String getEngine() {
    return engine;
  }

  public String getDescription() {
    return description;
  }

  public List<Map<String, Object>> getFunctions() {
    return functions;
  }

  public String getLibraryCode() {
    return libraryCode;
  }

  public static final Builder<LibraryInfo> LIBRARY_BUILDER = new Builder<LibraryInfo>() {
    @Override
    public LibraryInfo build(Object data) {
      List<Object> objectList = (List<Object>) data;
      String libname = STRING.build(objectList.get(1));
      String engine = STRING.build(objectList.get(3));
      String desc = STRING.build(objectList.get(5));
      List<Object> rawFunctions = (List<Object>) objectList.get(7);
      List<Map<String, Object>> functions = rawFunctions.stream().map(o -> ENCODED_OBJECT_MAP.build(o)).collect(Collectors.toList());
      if (objectList.size() <= 8) {
        return new LibraryInfo(libname, engine, desc, functions);
      }
      String code = STRING.build(objectList.get(9));
      return new LibraryInfo(libname, engine, desc, functions, code);
    }
  };

}
