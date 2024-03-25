package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.graph.ResultSet;

public class UnifiedJedisGraphCommandsTest extends UnifiedJedisTestBase {

  @Test
  public void testGraphQuery() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphQuery(name, query)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphQuery(name, query);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphQuery(name, query);
  }

  @Test
  public void testGraphReadonlyQuery() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphReadonlyQuery(name, query)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphReadonlyQuery(name, query);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphReadonlyQuery(name, query);
  }

  @Test
  public void testGraphQueryWithTimeout() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    long timeout = 1000L;
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphQuery(name, query, timeout)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphQuery(name, query, timeout);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphQuery(name, query, timeout);
  }

  @Test
  public void testGraphReadonlyQueryWithTimeout() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    long timeout = 1000L;
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphReadonlyQuery(name, query, timeout)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphReadonlyQuery(name, query, timeout);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphReadonlyQuery(name, query, timeout);
  }

  @Test
  public void testGraphQueryWithParams() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphQuery(name, query, params)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphQuery(name, query, params);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphQuery(name, query, params);
  }

  @Test
  public void testGraphReadonlyQueryWithParams() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphReadonlyQuery(name, query, params)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphReadonlyQuery(name, query, params);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphReadonlyQuery(name, query, params);
  }

  @Test
  public void testGraphQueryWithParamsAndTimeout() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    long timeout = 1000L;
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphQuery(name, query, params, timeout)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphQuery(name, query, params, timeout);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphQuery(name, query, params, timeout);
  }

  @Test
  public void testGraphReadonlyQueryWithParamsAndTimeout() {
    String name = "graph";
    String query = "MATCH (n) RETURN n";
    Map<String, Object> params = new HashMap<>();
    params.put("param1", "value1");
    long timeout = 1000L;
    ResultSet expectedResponse = mock(ResultSet.class);

    when(graphCommandObjects.graphReadonlyQuery(name, query, params, timeout)).thenReturn(resultSetCommandObject);
    when(commandExecutor.executeCommand(resultSetCommandObject)).thenReturn(expectedResponse);

    ResultSet result = unifiedJedis.graphReadonlyQuery(name, query, params, timeout);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(resultSetCommandObject);
    verify(graphCommandObjects).graphReadonlyQuery(name, query, params, timeout);
  }

  @Test
  public void testGraphDelete() {
    String name = "graph";
    String expectedResponse = "OK";

    when(graphCommandObjects.graphDelete(name)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = unifiedJedis.graphDelete(name);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(graphCommandObjects).graphDelete(name);
  }

  @Test
  public void testGraphProfile() {
    String graphName = "graph";
    String query = "MATCH (n) RETURN n";
    List<String> expectedResponse = Arrays.asList("Profile line 1", "Profile line 2");

    when(commandObjects.graphProfile(graphName, query)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = unifiedJedis.graphProfile(graphName, query);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).graphProfile(graphName, query);
  }

  @Test
  public void testGraphList() {
    List<String> expectedResponse = Arrays.asList("graph1", "graph2");

    when(commandObjects.graphList()).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = unifiedJedis.graphList();

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).graphList();
  }

  @Test
  public void testGraphExplain() {
    String graphName = "graph";
    String query = "MATCH (n) RETURN n";
    List<String> expectedResponse = Arrays.asList("Explain line 1", "Explain line 2");

    when(commandObjects.graphExplain(graphName, query)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = unifiedJedis.graphExplain(graphName, query);

    assertThat(result, sameInstance(expectedResponse));
    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).graphExplain(graphName, query);
  }

  @Test
  public void testGraphSlowlog() {
    String graphName = "graph";
    List<List<Object>> expectedResponse = Arrays.asList(
        Arrays.asList("Slowlog line 1", 123),
        Arrays.asList("Slowlog line 2", 456)
    );

    when(commandObjects.graphSlowlog(graphName)).thenReturn(listListObjectCommandObject);
    when(commandExecutor.executeCommand(listListObjectCommandObject)).thenReturn(expectedResponse);

    List<List<Object>> result = unifiedJedis.graphSlowlog(graphName);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(listListObjectCommandObject);
    verify(commandObjects).graphSlowlog(graphName);
  }

  @Test
  public void testGraphConfigSet() {
    String configName = "TIMEOUT";
    Object value = 1000;
    String expectedResponse = "OK";

    when(commandObjects.graphConfigSet(configName, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = unifiedJedis.graphConfigSet(configName, value);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).graphConfigSet(configName, value);
  }

  @Test
  public void testGraphConfigGet() {
    String configName = "TIMEOUT";
    Map<String, Object> expectedResponse = Collections.singletonMap(configName, 1000);

    when(commandObjects.graphConfigGet(configName)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = unifiedJedis.graphConfigGet(configName);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).graphConfigGet(configName);
  }

}
