package redis.clients.jedis.resps;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.RedisInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibraryInfoTest {

  private static Object parseRespResponse(String respResponse) {
    InputStream is = new ByteArrayInputStream(respResponse.getBytes());
    return Protocol.read(new RedisInputStream(is));
  }

  @Test
  public void buildLibraryInfoResp2Standard() {
    // Simulate standard RESP2 response for FUNCTION LIST (without WITHCODE)
    // Format: [library_name, <value>, engine, <value>, functions, <value>]
    String respResponse = "*6\r\n" + "$12\r\nlibrary_name\r\n" + "$5\r\nmylib\r\n"
        + "$6\r\nengine\r\n" + "$3\r\nLUA\r\n" + "$9\r\nfunctions\r\n" + "*1\r\n" + // functions
                                                                                    // array with 1
                                                                                    // element
        "*6\r\n" + // function info with 3 key-value pairs
        "$4\r\nname\r\n" + "$6\r\nmyfunc\r\n" + "$11\r\ndescription\r\n" + "$-1\r\n" + // null
                                                                                       // description
        "$5\r\nflags\r\n" + "*0\r\n"; // empty flags array

    Object data = parseRespResponse(respResponse);
    LibraryInfo result = LibraryInfo.LIBRARY_INFO.build(data);

    assertNotNull(result);
    assertEquals("mylib", result.getLibraryName());
    assertEquals("LUA", result.getEngine());
    assertNotNull(result.getFunctions());
    assertEquals(1, result.getFunctions().size());
    assertEquals("myfunc", result.getFunctions().get(0).get("name"));
    assertNull(result.getLibraryCode());
  }

  @Test
  public void buildLibraryInfoResp2WithCode() {
    // Simulate RESP2 response for FUNCTION LIST WITHCODE
    // Format: [library_name, <value>, engine, <value>, functions, <value>, library_code, <value>]
    String respResponse = "*8\r\n" + "$12\r\nlibrary_name\r\n" + "$5\r\nmylib\r\n"
        + "$6\r\nengine\r\n" + "$3\r\nLUA\r\n" + "$9\r\nfunctions\r\n" + "*1\r\n" + // functions
                                                                                    // array with 1
                                                                                    // element
        "*6\r\n" + // function info with 3 key-value pairs
        "$4\r\nname\r\n" + "$6\r\nmyfunc\r\n" + "$11\r\ndescription\r\n" + "$-1\r\n" + // null
                                                                                       // description
        "$5\r\nflags\r\n" + "*0\r\n" + // empty flags array
        "$12\r\nlibrary_code\r\n"
        + "$50\r\n#!LUA name=mylib\nredis.register_function('myfunc')\r\n";

    Object data = parseRespResponse(respResponse);
    LibraryInfo result = LibraryInfo.LIBRARY_INFO.build(data);

    assertNotNull(result);
    assertEquals("mylib", result.getLibraryName());
    assertEquals("LUA", result.getEngine());
    assertNotNull(result.getFunctions());
    assertEquals(1, result.getFunctions().size());
    assertEquals("myfunc", result.getFunctions().get(0).get("name"));
    assertNotNull(result.getLibraryCode());
    assertTrue(result.getLibraryCode().contains("#!LUA name=mylib"));
  }

  @Test
  public void buildLibraryInfoResp2WithExtraConsistentField() {
    // Simulate Redis Enterprise RESP2 response with extra "consistent" field
    // This is the bug scenario from CAE-2120
    // Format: [library_name, <value>, engine, <value>, consistent, <value>, functions, <value>]
    String respResponse = "*8\r\n" + "$12\r\nlibrary_name\r\n" + "$5\r\nmylib\r\n"
        + "$6\r\nengine\r\n" + "$3\r\nLUA\r\n" + "$10\r\nconsistent\r\n" + ":1\r\n" + // consistent
                                                                                      // value
                                                                                      // (integer)
        "$9\r\nfunctions\r\n" + "*1\r\n" + // functions array with 1 element
        "*6\r\n" + // function info with 3 key-value pairs
        "$4\r\nname\r\n" + "$6\r\nmyfunc\r\n" + "$11\r\ndescription\r\n" + "$-1\r\n" + // null
                                                                                       // description
        "$5\r\nflags\r\n" + "*0\r\n"; // empty flags array

    Object data = parseRespResponse(respResponse);
    LibraryInfo result = LibraryInfo.LIBRARY_INFO.build(data);

    assertNotNull(result);
    assertEquals("mylib", result.getLibraryName());
    assertEquals("LUA", result.getEngine());
    assertNotNull(result.getFunctions());
    assertEquals(1, result.getFunctions().size());
    assertEquals("myfunc", result.getFunctions().get(0).get("name"));
    assertNull(result.getLibraryCode());
  }

  @Test
  public void buildLibraryInfoResp2WithExtraConsistentFieldAndCode() {
    // Simulate Redis Enterprise RESP2 response with extra "consistent" field and WITHCODE
    // Format: [library_name, <value>, engine, <value>, consistent, <value>, functions, <value>,
    // library_code, <value>]
    String respResponse = "*10\r\n" + "$12\r\nlibrary_name\r\n" + "$5\r\nmylib\r\n"
        + "$6\r\nengine\r\n" + "$3\r\nLUA\r\n" + "$10\r\nconsistent\r\n" + ":1\r\n" + // consistent
                                                                                      // value
                                                                                      // (integer)
        "$9\r\nfunctions\r\n" + "*1\r\n" + // functions array with 1 element
        "*6\r\n" + // function info with 3 key-value pairs
        "$4\r\nname\r\n" + "$6\r\nmyfunc\r\n" + "$11\r\ndescription\r\n" + "$-1\r\n" + // null
                                                                                       // description
        "$5\r\nflags\r\n" + "*0\r\n" + // empty flags array
        "$12\r\nlibrary_code\r\n"
        + "$50\r\n#!LUA name=mylib\nredis.register_function('myfunc')\r\n";

    Object data = parseRespResponse(respResponse);
    LibraryInfo result = LibraryInfo.LIBRARY_INFO.build(data);

    assertNotNull(result);
    assertEquals("mylib", result.getLibraryName());
    assertEquals("LUA", result.getEngine());
    assertNotNull(result.getFunctions());
    assertEquals(1, result.getFunctions().size());
    assertEquals("myfunc", result.getFunctions().get(0).get("name"));
    assertNotNull(result.getLibraryCode());
    assertTrue(result.getLibraryCode().contains("#!LUA name=mylib"));
  }
}
