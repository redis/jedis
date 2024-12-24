package redis.clients.jedis.authentication;

public interface AuthXEventListener {

    static AuthXEventListener NOOP_LISTENER = new AuthXEventListener() {

        @Override
        public void onIdentityProviderError(Exception reason) {
        }

        @Override
        public void onConnectionAuthenticationError(Exception reason) {
        }

    };

    public void onIdentityProviderError(Exception reason);

    public void onConnectionAuthenticationError(Exception reason);

}
