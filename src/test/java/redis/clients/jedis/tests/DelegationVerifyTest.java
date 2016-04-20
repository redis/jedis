package redis.clients.jedis.tests;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.util.Hashing;
import redis.clients.util.ShardInfo;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DelegationVerifyTest {

  private PodamFactory factory = new PodamFactoryImpl();

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

    // FIXME: check we can remove this when we can instantiate byte[], String[] from podam.
    when(mockedShardedJedis.getShard(isNull(byte[].class))).thenReturn(mockedJedis);
    when(mockedShardedJedis.getShard(isNull(String.class))).thenReturn(mockedJedis);

    when(mockedShardedJedis.getShardInfo(any(byte[].class))).thenReturn(dummyShardInfo);
    when(mockedShardedJedis.getShardInfo(anyString())).thenReturn(dummyShardInfo);

    // FIXME: check we can remove this when we can instantiate byte[], String[] from podam.
    when(mockedShardedJedis.getShardInfo(isNull(byte[].class))).thenReturn(dummyShardInfo);
    when(mockedShardedJedis.getShardInfo(isNull(String.class))).thenReturn(dummyShardInfo);

    ShardedJedisPipeline pipeline = new ShardedJedisPipeline();
    pipeline.setShardedJedis(mockedShardedJedis);

    verifyAllCommandMethodsAreDelegatingProperly(pipeline, mockedClient);
  }

  private void verifyAllCommandMethodsAreDelegatingProperly(Object instance, Object mockedDelegator)
      throws InvocationTargetException, IllegalAccessException {
    List<Class<?>> allInterfaces = ClassUtils.getAllInterfaces(instance.getClass());
    for (Class<?> inter : allInterfaces) {
      if (!inter.getCanonicalName().startsWith("redis.clients.jedis.commands")) {
        continue;
      }

      for (Method method : inter.getMethods()) {
        if (needToSkipMethod(method)) {
          System.err.println("Skipping method due to some issues : " + method);
          continue;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        int idx = 0;
        for (Class<?> parameterType : parameterTypes) {
          // FIXME: should instantiate byte[], String[], byte[][], [I(??), Map (possible?)
          parameters[idx++] = factory.manufacturePojo(parameterType);
        }

        method.invoke(instance, parameters);

        Method matchingMethod = findMatchingMethod(mockedDelegator.getClass(), method);
        matchingMethod.invoke(verify(mockedDelegator), parameters);
        reset(mockedDelegator);
      }
    }
  }

  private void injectMockedClientToJedis(Client mockedClient, Jedis jedis) {
    // Reflection to force inject mock anyway
    Field clientField = ReflectionUtils.findField(Jedis.class, "client");

    ReflectionUtils.makeAccessible(clientField);
    ReflectionUtils.setField(clientField, jedis, mockedClient);
  }

  private void injectMockedResourceToShardedJedis(List<JedisShardInfo> shardInfos, Jedis mockJedis,
      ShardedJedis shardedJedis) {
    // Reflection to force inject mock anyway
    Field resourcesField = ReflectionUtils.findField(ShardedJedis.class, "resources");

    Map<ShardInfo<Jedis>, Jedis> mockResources = new LinkedHashMap<ShardInfo<Jedis>, Jedis>();
    mockResources.put(shardInfos.get(0), mockJedis);

    ReflectionUtils.makeAccessible(resourcesField);
    ReflectionUtils.setField(resourcesField, shardedJedis, mockResources);

    // FIXME: check we can remove this when we can instantiate byte[] from podam.
    Field algoField = ReflectionUtils.findField(ShardedJedis.class, "algo");
    ReflectionUtils.makeAccessible(algoField);
    ReflectionUtils.setField(algoField, shardedJedis, new Hashing() {
      @Override
      public long hash(String key) {
        return 0;
      }

      @Override
      public long hash(byte[] key) {
        return 0;
      }
    });
  }

  private boolean needToSkipMethod(Method method) {
    // FIXME: we should replace vararg to List or Collection or Iterator or sth. to get rid of ambiguity. It's already reported from Scala users.

    List<String> methodNamesToSkip = new ArrayList<String>();
    // parameter ambiguity
    methodNamesToSkip.add("del");
    methodNamesToSkip.add("blpop");
    methodNamesToSkip.add("brpop");
    // parameter ambiguity

    return methodNamesToSkip.contains(method.getName());
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
