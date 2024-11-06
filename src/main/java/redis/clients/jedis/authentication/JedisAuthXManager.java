package redis.clients.jedis.authentication;

import redis.clients.authentication.core.AuthXManager;
import redis.clients.authentication.core.Token;
import redis.clients.authentication.core.TokenListener;
import redis.clients.authentication.core.TokenManager;

public class JedisAuthXManager extends AuthXManager {
    private TokenListener listener;

    public JedisAuthXManager(TokenManager tokenManager) {
        super(tokenManager);
    }

    public void setListener(TokenListener listener) {
        this.listener = listener;
    }

    @Override
    public void authenticateConnections(Token token) {
        super.authenticateConnections(token);
        if (listener != null) {
            listener.onTokenRenewed(token);
        }
    }
}