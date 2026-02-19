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

      String[] libname = { null };
      String[] enginename = { null };
      String[] librarycode = { null };
      List[] functions = { null };

      if (list.get(0) instanceof KeyValue) {
        // RESP3 format: list of KeyValue objects
        for (KeyValue kv : (List<KeyValue>) list) {
          processField(kv.getKey(), kv.getValue(), libname, enginename, functions, librarycode);
        }
      } else {
        // RESP2 format: flat list with alternating key-value pairs
        // Note: Redis Enterprise may include extra fields like "consistent"
        for (int i = 0; i + 1 < list.size(); i += 2) {
          processField(list.get(i), list.get(i + 1), libname, enginename, functions, librarycode);
        }
      }

      return new LibraryInfo(libname[0], enginename[0], functions[0], librarycode[0]);
    }

    private void processField(Object key, Object value, String[] libname, String[] enginename,
        List[] functions, String[] librarycode) {
      switch (BuilderFactory.STRING.build(key)) {
        case "library_name":
          libname[0] = BuilderFactory.STRING.build(value);
          break;
        case "engine":
          enginename[0] = BuilderFactory.STRING.build(value);
          break;
        case "functions":
          functions[0] = ((List<Object>) value).stream().map(ENCODED_OBJECT_MAP::build)
              .collect(Collectors.toList());
          break;
        case "library_code":
          librarycode[0] = BuilderFactory.STRING.build(value);
          break;
      }
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
