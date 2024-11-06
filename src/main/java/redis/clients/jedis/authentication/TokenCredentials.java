package redis.clients.jedis.authentication;

import redis.clients.authentication.core.Token;
import redis.clients.jedis.RedisCredentials;

public class TokenCredentials implements RedisCredentials {
    private final String user;
    private final char[] password;

    public TokenCredentials(Token token) {
        user = token.tryGet("oid");
        password = token.getValue().toCharArray();
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public char[] getPassword() {
        return password;
    }
}