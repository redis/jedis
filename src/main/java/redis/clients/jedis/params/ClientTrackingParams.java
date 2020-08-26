package redis.clients.jedis.params;

import redis.clients.jedis.Client;

public class ClientTrackingParams extends Params{

    private static final String REDIRECT = "REDIRECT";
    private static final String BCAST = "BCAST";
    private static final String PREFIX = "PREFIX";
    private static final String OPTIN = "OPTIN";
    private static final String OPTOUT = "OPTOUT";
    private static final String NOLOOP = "NOLOOP";

    public static ClientTrackingParams clientTrackingParams() {
        return new ClientTrackingParams();
    }

    public ClientTrackingParams redirect(Long clientId) {
        addParam(REDIRECT, Long.toString(clientId));
        return this;
    }

    public ClientTrackingParams prefix(String prefix) {
        addParam(PREFIX, prefix);
        return this;
    }

    public ClientTrackingParams bcast() {
        addParam(BCAST);
        return this;
    }

    public ClientTrackingParams optin() {
        addParam(OPTIN);
        return this;
    }

    public ClientTrackingParams optout() {
        addParam(OPTOUT);
        return this;
    }

    public ClientTrackingParams noloop() {
        addParam(NOLOOP);
        return this;
    }

}
