package redis.clients.jedis.authentication;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenAuthConfig;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;
import redis.clients.jedis.Connection;
import redis.clients.jedis.RedisCredentials;

public final class AuthXManager implements Supplier<RedisCredentials> {

    private static final Logger log = LoggerFactory.getLogger(AuthXManager.class);

    private TokenManager tokenManager;
    private List<WeakReference<Connection>> connections = Collections
            .synchronizedList(new ArrayList<>());
    private Token currentToken;
    private AuthXEventListener listener = AuthXEventListener.NOOP_LISTENER;
    private List<Consumer<Token>> postAuthenticateHooks = new ArrayList<>();
    private AtomicReference<CompletableFuture<Void>> uniqueStarterTask = new AtomicReference<>();

    protected AuthXManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public AuthXManager(TokenAuthConfig tokenAuthConfig) {
        this(new TokenManager(tokenAuthConfig.getIdentityProviderConfig().getProvider(),
                tokenAuthConfig.getTokenManagerConfig()));
    }

    public void start() {
        Future<Void> safeStarter = safeStart(this::tokenManagerStart);
        try {
            safeStarter.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("AuthXManager failed to start!", e);
            throw new JedisAuthenticationException("AuthXManager failed to start!",
                    (e instanceof ExecutionException) ? e.getCause() : e);
        }
    }

    private Future<Void> safeStart(Runnable starter) {
        if (uniqueStarterTask.compareAndSet(null, new CompletableFuture<Void>())) {
            try {
                starter.run();
                uniqueStarterTask.get().complete(null);
            } catch (Exception e) {
                uniqueStarterTask.get().completeExceptionally(e);
            }
        }
        return uniqueStarterTask.get();
    }

    private void tokenManagerStart() {
        tokenManager.start(new TokenListener() {
            @Override
            public void onTokenRenewed(Token token) {
                currentToken = token;
                authenticateConnections(token);
            }

            @Override
            public void onError(Exception reason) {
                listener.onIdentityProviderError(reason);
            }
        }, true);
    }

    public void authenticateConnections(Token token) {
        RedisCredentials credentialsFromToken = new TokenCredentials(token);
        for (WeakReference<Connection> connectionRef : connections) {
            Connection connection = connectionRef.get();
            if (connection != null) {
                connection.setCredentials(credentialsFromToken);
            } else {
                connections.remove(connectionRef);
            }
        }
        postAuthenticateHooks.forEach(hook -> hook.accept(token));
    }

    public Connection addConnection(Connection connection) {
        connections.add(new WeakReference<>(connection));
        return connection;
    }

    public void stop() {
        tokenManager.stop();
    }

    public void setListener(AuthXEventListener listener) {
        if (listener != null) {
            this.listener = listener;
        }
    }

    public void addPostAuthenticationHook(Consumer<Token> postAuthenticateHook) {
        postAuthenticateHooks.add(postAuthenticateHook);
    }

    public void removePostAuthenticationHook(Consumer<Token> postAuthenticateHook) {
        postAuthenticateHooks.remove(postAuthenticateHook);
    }

    public AuthXEventListener getListener() {
        return listener;
    }

    @Override
    public RedisCredentials get() {
        return new TokenCredentials(this.currentToken);
    }

}