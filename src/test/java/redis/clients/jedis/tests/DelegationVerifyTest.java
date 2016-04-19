package redis.clients.jedis.tests;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;
import redis.clients.jedis.Client;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.Pipeline;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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

    List<Class<?>> allInterfacesForPipeline = ClassUtils.getAllInterfaces(Pipeline.class);
    for (Class<?> inter : allInterfacesForPipeline) {
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

        method.invoke(pipeline, parameters);

        Method matchingMethod = findMatchingMethod(mockClient.getClass(), method);
        matchingMethod.invoke(verify(mockClient), parameters);
        reset(mockClient);
      }
    }
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
