package redis.clients.jedis.params;

import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;

public class ClientTrackingParams extends Params{

    private static final String REDIRECT = Protocol.Keyword.REDIRECT.name();
    private static final String BCAST = Protocol.Keyword.BCAST.name();
    private static final String PREFIX = Protocol.Keyword.PREFIX.name();
    private static final String OPTIN = Protocol.Keyword.OPTIN.name();
    private static final String OPTOUT = Protocol.Keyword.OPTOUT.name();
    private static final String NOLOOP = Protocol.Keyword.NOLOOP.name();

    private boolean noLoop = false;

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
        noLoop = true;
        return this;
    }

    public boolean isNoLoop() {
        return noLoop;
    }
}
