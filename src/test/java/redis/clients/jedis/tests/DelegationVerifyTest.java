package redis.clients.jedis.tests;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.Client;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.commands.JedisClusterBinaryScriptingCommands;
import redis.clients.jedis.commands.JedisClusterScriptingCommands;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.ShardInfo;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

interface MockInitializer<T> {
  void initializeMock(T mockedInstance);
}

class HackedJedisCluster extends JedisCluster {
  public HackedJedisCluster(HostAndPort node) {
    super(node);
  }

  public void setConnectionHandler(JedisClusterConnectionHandler connectionHandler) {
    this.connectionHandler = connectionHandler;
  }
}

// http://huahsin68.blogspot.kr/2015/07/what-hell-wrong-instance-of.html
@PowerMockIgnore({"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(JedisClusterCRC16.class)
public class DelegationVerifyTest {
  public static class HelperPojo {
    private int[] integers;
    private byte[] bytes;
    private String[] strings;
    private byte[][] arrayOfBytes;

    public int[] getIntegers() {
      return integers;
    }

    public byte[] getBytes() {
      return bytes;
    }

    public void setBytes(byte[] bytes) {
      this.bytes = bytes;
    }

    public String[] getStrings() {
      return strings;
    }

    public void setIntegers(int[] integers) {
      this.integers = integers;
    }

    public void setStrings(String[] strings) {
      this.strings = strings;
    }

    public byte[][] getArrayOfBytes() {
      return arrayOfBytes;
    }

    public void setArrayOfBytes(byte[][] arrayOfBytes) {
      this.arrayOfBytes = arrayOfBytes;
    }
  }

  private PodamFactory factory = new PodamFactoryImpl();
  private HelperPojo helperPojo;

  @Before
  public void init() {
    helperPojo = factory.manufacturePojo(HelperPojo.class);
  }

  @Test
  public void testSanityOnPipeline() throws InvocationTargetException, IllegalAccessException, InstantiationException {
    Client mockClient = mock(Client.class);
    Pipeline pipeline = new Pipeline();
    pipeline.setClient(mockClient);

    verifyAllCommandMethodsAreDelegatingProperly(pipeline, mockClient);
  }

  @Test
  public void testSanityOnShardedJedis() throws Exception {
    List<JedisShardInfo> shardInfos = new ArrayList<JedisShardInfo>();
    shardInfos.add(new JedisShardInfo("localhost", 6379));

    Jedis mockedJedis = mock(Jedis.class);

    ShardedJedis shardedJedis = new ShardedJedis(shardInfos);
    injectMockedResourceToShardedJedis(shardInfos, mockedJedis, shardedJedis);

    verifyAllCommandMethodsAreDelegatingProperly(shardedJedis, mockedJedis);
  }

  @Test
  public void testSanityOnShardedJedisPipeline()
      throws InvocationTargetException, IllegalAccessException {
    Client mockedClient = mock(Client.class);
    Jedis mockedJedis = mock(Jedis.class);
    when(mockedJedis.getClient()).thenReturn(mockedClient);

    ShardedJedis mockedShardedJedis = mock(ShardedJedis.class);
    JedisShardInfo dummyShardInfo = new JedisShardInfo("localhost", 6379);
    when(mockedShardedJedis.getShard(any(byte[].class))).thenReturn(mockedJedis);
    when(mockedShardedJedis.getShard(anyString())).thenReturn(mockedJedis);
    when(mockedShardedJedis.getShardInfo(any(byte[].class))).thenReturn(dummyShardInfo);
    when(mockedShardedJedis.getShardInfo(anyString())).thenReturn(dummyShardInfo);

    ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
    pipeline.setShardedJedis(mockedShardedJedis);

    verifyAllCommandMethodsAreDelegatingProperly(pipeline, mockedClient);
  }

  @Test
  public void testSanityOnJedisCluster() throws InvocationTargetException, IllegalAccessException {
    HackedJedisCluster hackedJedisCluster = new HackedJedisCluster(HostAndPortUtil.getClusterServers().get(0));
    JedisClusterConnectionHandler connectionHandler = new JedisSlotBasedConnectionHandler(new HashSet<HostAndPort>(), new JedisPoolConfig(), 0);

    JedisClusterInfoCache mockedClusterInfoCache = mock(JedisClusterInfoCache.class);
    injectMockedCacheToJedisClusterConnectionHandler(mockedClusterInfoCache, connectionHandler);

    JedisPool mockedJedisPool = mock(JedisPool.class);
    Map<String, JedisPool> cacheNodes = new HashMap<String, JedisPool>();
    cacheNodes.put("placeholder", mockedJedisPool);
    Jedis mockedJedis = mock(Jedis.class);

    when(mockedJedisPool.getResource()).thenReturn(mockedJedis);
    when(mockedClusterInfoCache.getNode(anyString())).thenReturn(mockedJedisPool);
    when(mockedClusterInfoCache.getSlotPool(anyInt())).thenReturn(mockedJedisPool);
    when(mockedClusterInfoCache.getNodes()).thenReturn(cacheNodes);

    hackedJedisCluster.setConnectionHandler(connectionHandler);

    // Only reason to use Powermock: We should mock final static class now...
    mockStatic(JedisClusterCRC16.class);
    when(JedisClusterCRC16.getSlot(any(byte[].class))).thenReturn(0);
    when(JedisClusterCRC16.getSlot(anyString())).thenReturn(0);

    verifyAllCommandMethodsAreDelegatingProperly(hackedJedisCluster, mockedJedis,
        new MockInitializer<Jedis>() {
      @Override
      public void initializeMock(Jedis mockedInstance) {
        when(mockedInstance.ping()).thenReturn("PONG");
      }
    });
  }

  private void verifyAllCommandMethodsAreDelegatingProperly(Object instance, Object mockedDelegator)
      throws InvocationTargetException, IllegalAccessException {
    verifyAllCommandMethodsAreDelegatingProperly(instance, mockedDelegator, null);
  }

  private void verifyAllCommandMethodsAreDelegatingProperly(Object instance, Object mockedDelegator,
      MockInitializer initializer)
      throws InvocationTargetException, IllegalAccessException {

    List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(instance.getClass());
    for (Class<?> inter : allInterfaces) {
      if (!inter.getCanonicalName().startsWith("redis.clients.jedis.commands")) {
        continue;
      }

      for (Method method : inter.getMethods()) {
        if (needToSkipMethod(instance.getClass(), method)) {
          System.err.println("Skipping method due to some issues : " + method);
          continue;
        }

        if (initializer != null) {
          initializer.initializeMock(mockedDelegator);
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Object[] parameterValues = new Object[parameterTypes.length];

        for (int idx = 0 ; idx < parameterTypes.length ; idx++) {
          parameterValues[idx] = getDummyValue(parameterTypes[idx], genericParameterTypes[idx]);
        }

        method.invoke(instance, parameterValues);

        Method matchingMethod = findMatchingMethod(mockedDelegator.getClass(), method);
        matchingMethod.invoke(verify(mockedDelegator), parameterValues);
        reset(mockedDelegator);
      }
    }
  }

  private Object getDummyValue(Class<?> parameterType, Type genericParameterType) {
    if (parameterType.equals(byte[].class)) {
      return helperPojo.getBytes();
    } else if (parameterType.equals(String[].class)) {
      return helperPojo.getStrings();
    } else if (parameterType.equals(int[].class)) {
      return helperPojo.getIntegers();
    } else if (parameterType.equals(byte[][].class)) {
      return helperPojo.getArrayOfBytes();
    } else if (parameterType.equals(Map.class)) {
      // just give up, no need to add actual values.
      return new HashMap();
    } else if (parameterType.equals(List.class)) {
      if (genericParameterType instanceof ParameterizedType) {
        Type[] actualTypeArguments = ((ParameterizedType)genericParameterType).getActualTypeArguments();
        Class actualTypeArgumentClass = (Class) actualTypeArguments[0];

        List list = new ArrayList();
        Object value = getDummyValue(actualTypeArgumentClass, actualTypeArgumentClass);

        // push plenty of arguments so that it lowers the probabilities of ArrayIndexOutOfException
        for (int i = 0 ; i < 1000 ; i++) {
          list.add(value);
        }

        return list;
      } else {
        // no generic parameter information. give up.
        return new ArrayList();
      }
    } else {
      return factory.manufacturePojo(parameterType);
    }
  }

  private void injectMockedResourceToShardedJedis(List<JedisShardInfo> shardInfos, Jedis mockJedis,
      ShardedJedis shardedJedis) {
    // Reflection to force inject mock anyway
    Field resourcesField = ReflectionUtils.findField(ShardedJedis.class, "resources");

    Map<ShardInfo<Jedis>, Jedis> mockResources = new LinkedHashMap<ShardInfo<Jedis>, Jedis>();
    mockResources.put(shardInfos.get(0), mockJedis);

    ReflectionUtils.makeAccessible(resourcesField);
    ReflectionUtils.setField(resourcesField, shardedJedis, mockResources);
  }

  private void injectMockedCacheToJedisClusterConnectionHandler(JedisClusterInfoCache mockedCache,
      JedisClusterConnectionHandler connectionHandler) {
    // Reflection to force inject mock anyway
    Field cacheField = ReflectionUtils.findField(JedisClusterConnectionHandler.class, "cache");

    ReflectionUtils.makeAccessible(cacheField);
    ReflectionUtils.setField(cacheField, connectionHandler, mockedCache);
  }

  private boolean needToSkipMethod(Class classToTest, Method method) {
    // FIXME: we should replace vararg to List or Collection or Iterator or sth. to get rid of ambiguity. It's already reported from Scala users.

    List<String> methodNamesToSkip = new ArrayList<String>();
    // parameter ambiguity
    methodNamesToSkip.add("del");
    methodNamesToSkip.add("blpop");
    methodNamesToSkip.add("brpop");
    // parameter ambiguity end

    if (methodNamesToSkip.contains(method.getName())) {
      return true;
    }

    if (classToTest.equals(HackedJedisCluster.class)) {
      // We can't test JedisClusterScriptingCommands since it uses different signature compared to Jedis.
      // it should be checked manually
      try {
        JedisClusterScriptingCommands.class.getMethod(method.getName(), method.getParameterTypes());
        return true;
      } catch (NoSuchMethodException e) {
        // continue
      }

      // We can't test JedisClusterBinaryScriptingCommands since it uses different signature compared to Jedis.
      // it should be checked manually
      try {
        JedisClusterBinaryScriptingCommands.class.getMethod(method.getName(), method.getParameterTypes());
        return true;
      } catch (NoSuchMethodException e) {
        // continue
      }

    }

    return false;
  }

  private <T> Method findMatchingMethod(Class mockedClass, Method method) {
    for (Method methodMock : mockedClass.getMethods()) {
      if (methodMock.getName().equals(method.getName()) &&
          Arrays.equals(methodMock.getParameterTypes(), method.getParameterTypes())) {
        return methodMock;
      }
    }

    throw new RuntimeException("No matching method for " + method);
  }
}
