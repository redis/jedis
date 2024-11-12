package redis.clients.jedis.authentication;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;
import redis.clients.jedis.Connection;
import redis.clients.jedis.RedisCredentials;

public class JedisAuthXManager implements Supplier<RedisCredentials> {

    private static final Logger log = LoggerFactory.getLogger(JedisAuthXManager.class);

    private TokenManager tokenManager;
    private List<WeakReference<Connection>> connections = Collections
            .synchronizedList(new ArrayList<>());
    private Token currentToken;
    private AuthenticationListener listener;

    public interface AuthenticationListener {
        public void onAuthenticate(Token token);
    }

    public JedisAuthXManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public JedisAuthXManager(TokenAuthConfig tokenAuthConfig) {
        this(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()));
    }

    public void start(boolean blockForInitialToken)
            throws InterruptedException, ExecutionException, TimeoutException {

        tokenManager.start(new TokenListener() {
            @Override
            public void onTokenRenewed(Token token) {
                currentToken = token;
                authenticateConnections(token);
            }

            @Override
            public void onError(Exception reason) {
                JedisAuthXManager.this.onError(reason);
            }
        }, blockForInitialToken);
    }

    public void authenticateConnections(Token token) {
        RedisCredentials credentialsFromToken = new TokenCredentials(token);
        for (WeakReference<Connection> connectionRef : connections) {
            Connection connection = connectionRef.get();
            if (connection != null) {
                try {
                    connection.setCredentials(credentialsFromToken);
                } catch (Exception e) {
                    log.error("Failed to authenticate connection!", e);
                }
            } else {
                connections.remove(connectionRef);
            }
        }
        if (listener != null) {
            listener.onAuthenticate(token);
        }
    }

    public void onError(Exception reason) {
        throw new JedisAuthenticationException(
                "Token request/renewal failed with message:" + reason.getMessage(), reason);
    }

    public Connection addConnection(Connection connection) {
        connections.add(new WeakReference<>(connection));
        return connection;
    }

    public void stop() {
        tokenManager.stop();
    }

    public void setListener(AuthenticationListener listener) {
        this.listener = listener;
    }

    @Override
    public RedisCredentials get() {
        return new TokenCredentials(this.currentToken);
    }

}