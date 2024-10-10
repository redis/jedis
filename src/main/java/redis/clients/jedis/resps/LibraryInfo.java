package redis.clients.jedis.resps;

import static redis.clients.jedis.BuilderFactory.STRING;
import static redis.clients.jedis.BuilderFactory.ENCODED_OBJECT_MAP;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.KeyValue;

//[library_name=mylib, engine=LUA, functions=[[name=myfunc, description=null, flags=[]]], library_code=#!LUA name=mylib 
// redis.register_function('myfunc', function(keys, args) return args[1] end)]
public class LibraryInfo {

  private final String libraryName;
  private final String engine;
  private final List<Map<String, Object>> functions;
  private final String libraryCode;

  public LibraryInfo(String libraryName, String engineName, List<Map<String, Object>> functions) {
    this(libraryName, engineName, functions, null);
  }

  public LibraryInfo(String libraryName, String engineName, List<Map<String, Object>> functions, String code) {
    this.libraryName = libraryName;
    this.engine = engineName;
    this.functions = functions;
    this.libraryCode = code;
  }

  public String getLibraryName() {
    return libraryName;
  }

  public String getEngine() {
    return engine;
  }

  public List<Map<String, Object>> getFunctions() {
    return functions;
  }

  public String getLibraryCode() {
    return libraryCode;
  }

  public static final Builder<LibraryInfo> LIBRARY_INFO = new Builder<LibraryInfo>() {
    @Override
    public LibraryInfo build(Object data) {
      if (data == null) return null;
      List list = (List) data;
      if (list.isEmpty()) return null;

      if (list.get(0) instanceof KeyValue) {
        String libname = null, enginename = null, librarycode = null;
        List<Map<String, Object>> functions = null;
        for (KeyValue kv : (List<KeyValue>) list) {
          switch (BuilderFactory.STRING.build(kv.getKey())) {
            case "library_name":
              libname = BuilderFactory.STRING.build(kv.getValue());
              break;
            case "engine":
              enginename = BuilderFactory.STRING.build(kv.getValue());
              break;
            case "functions":
              functions = ((List<Object>) kv.getValue()).stream().map(o -> ENCODED_OBJECT_MAP.build(o)).collect(Collectors.toList());
              break;
            case "library_code":
              librarycode = BuilderFactory.STRING.build(kv.getValue());
              break;
          }
        }
        return new LibraryInfo(libname, enginename, functions, librarycode);
      }

      String libname = STRING.build(list.get(1));
      String engine = STRING.build(list.get(3));
      List<Object> rawFunctions = (List<Object>) list.get(5);
      List<Map<String, Object>> functions = rawFunctions.stream().map(o -> ENCODED_OBJECT_MAP.build(o)).collect(Collectors.toList());
      if (list.size() <= 6) {
        return new LibraryInfo(libname, engine, functions);
      }
      String code = STRING.build(list.get(7));
      return new LibraryInfo(libname, engine, functions, code);
    }
  };

  /**
   * @deprecated Use {@link LibraryInfo#LIBRARY_INFO}.
   */
  @Deprecated
  public static final Builder<LibraryInfo> LIBRARY_BUILDER = LIBRARY_INFO;

  public static final Builder<List<LibraryInfo>> LIBRARY_INFO_LIST = new Builder<List<LibraryInfo>>() {
    @Override
    public List<LibraryInfo> build(Object data) {
      List<Object> list = (List<Object>) data;
      return list.stream().map(o -> LibraryInfo.LIBRARY_INFO.build(o)).collect(Collectors.toList());
    }
  };

}
