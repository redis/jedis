package redis.clients.jedis.authentication;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

    protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

    @Test
    public void testJedisPooledAuth() {
        String user = "default";
        String password = endpoint.getPassword();

        IdentityProvider idProvider = mock(IdentityProvider.class);
        when(idProvider.requestToken())
                .thenReturn(new SimpleToken(password, new Date(System.currentTimeMillis() + 100000),
                        new Date(), Collections.singletonMap("oid", user)));

        IdentityProviderConfig idProviderConfig = mock(IdentityProviderConfig.class);
        when(idProviderConfig.getProvider()).thenReturn(idProvider);

        TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder()
                .identityProviderConfig(idProviderConfig).expirationRefreshRatio(0.8F)
                .lowerRefreshBoundMillis(10000).tokenRequestExecutionTimeoutInMs(1000).build();

        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .tokenAuthConfig(tokenAuthConfig).build();

        try (MockedStatic<Protocol> mockedStatic = Mockito.mockStatic(Protocol.class)) {
            ArgumentCaptor<CommandArguments> captor = ArgumentCaptor
                    .forClass(CommandArguments.class);

            try (JedisPooled jedis = new JedisPooled(endpoint.getHostAndPort(), clientConfig)) {
                jedis.get("key1");
            }

            // Verify that the static method was called
            mockedStatic.verify(() -> Protocol.sendCommand(any(), captor.capture()),
                Mockito.atLeast(4));

            CommandArguments commandArgs = captor.getAllValues().get(0);
            List<byte[]> args = StreamSupport.stream(commandArgs.spliterator(), false)
                    .map(Rawable::getRaw).collect(Collectors.toList());

            assertThat(args,
                contains(Protocol.Command.AUTH.getRaw(), user.getBytes(), password.getBytes()));

            List<ProtocolCommand> cmds = captor.getAllValues().stream()
                    .map(item -> item.getCommand()).collect(Collectors.toList());
            assertEquals(Arrays.asList(Command.AUTH, Command.CLIENT, Command.CLIENT, Command.GET),
                cmds);
        }
    }
}
