package redis.clients.jedis.authentication;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.IdentityProvider;
import redis.clients.authentication.core.IdentityProviderConfig;
import redis.clients.authentication.core.SimpleToken;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.jedis.CommandArguments;
/*  */
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;

public class TokenBasedAuthenticationIntegrationTests {
  private static final Logger log = LoggerFactory
      .getLogger(TokenBasedAuthenticationIntegrationTests.class);

  private static EndpointConfig endpointConfig;

  @BeforeClass
  public static void before() {
    try {
      endpointConfig = HostAndPorts.getRedisEndpoint("standalone0");
    } catch (IllegalArgumentException e) {
      try {
        endpointConfig = HostAndPorts.getRedisEndpoint("standalone");
      } catch (IllegalArgumentException ex) {
        log.warn("Skipping test because no Redis endpoint is configured");
        org.junit.Assume.assumeTrue(false);
      }
    }
  }

  @Test
  public void testJedisPooledForInitialAuth() {
    String user = "default";
    String password = endpointConfig.getPassword();

    IdentityProvider idProvider = mock(IdentityProvider.class);
    when(idProvider.requestToken())
        .thenReturn(new SimpleToken(password, System.currentTimeMillis() + 100000,
            System.currentTimeMillis(), Collections.singletonMap("oid", user)));

    IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
    when(idProviderConfig.getProvider()).thenReturn(idProvider);

    TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
        .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
        .lowerRefreshBoundMillis(10000).tokenRequestExecTimeoutInMs(1000).build();

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .authXManager(new AuthXManager(tokenAuthConfig)).build();

    try (MockedStatic<Protocol> mockedStatic = Mockito.mockStatic(Protocol.class)) {
      ArgumentCaptor<CommandArguments> captor = ArgumentCaptor.forClass(CommandArguments.class);

      try (JedisPooled jedis = new JedisPooled(endpointConfig.getHostAndPort(), clientConfig)) {
        jedis.get("key1");
      }

      // Verify that the static method was called
      mockedStatic.verify(() -> Protocol.sendCommand(any(), captor.capture()), Mockito.atLeast(4));

      CommandArguments commandArgs = captor.getAllValues().get(0);
      List<byte[]> args = StreamSupport.stream(commandArgs.spliterator(), false)
          .map(Rawable::getRaw).collect(Collectors.toList());

      assertThat(args,
        contains(Protocol.Command.AUTH.getRaw(), user.getBytes(), password.getBytes()));

      List<ProtocolCommand> cmds = captor.getAllValues().stream().map(item -> item.getCommand())
          .collect(Collectors.toList());
      assertEquals(Arrays.asList(Command.AUTH, Command.CLIENT, Command.CLIENT, Command.GET), cmds);
    }
  }
}
