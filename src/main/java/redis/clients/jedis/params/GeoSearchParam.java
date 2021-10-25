package redis.clients.jedis.params;

import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoSearchParam extends GeoRadiusParam {
    private static final String BYRADIUS = "byradius";
    private static final String BYBOX = "bybox";

    public GeoSearchParam() {
    }

    public static GeoSearchParam geoSearchParam() { return new GeoSearchParam(); }


    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<>();
        Collections.addAll(byteParams, args);

        if (contains(BYRADIUS)) {
            byteParams.add(SafeEncoder.encode(BYRADIUS));
            byteParams.add(((String) getParam(BYRADIUS)).getBytes());
        }

        if (contains(BYBOX)) {
            byteParams.add(SafeEncoder.encode(BYBOX));
            byteParams.add(((String) getParam(BYBOX)).getBytes());
        }

        if (contains(WITHCOORD)) {
            byteParams.add(SafeEncoder.encode(WITHCOORD));
        }
        if (contains(WITHDIST)) {
            byteParams.add(SafeEncoder.encode(WITHDIST));
        }
        if (contains(WITHHASH)) {
            byteParams.add(SafeEncoder.encode(WITHHASH));
        }

        if (contains(COUNT)) {
            byteParams.add(SafeEncoder.encode(COUNT));
            byteParams.add(Protocol.toByteArray((int) getParam(COUNT)));
            if (contains(ANY)) {
                byteParams.add(SafeEncoder.encode(ANY));
            }
        }

        if (contains(ASC)) {
            byteParams.add(SafeEncoder.encode(ASC));
        } else if (contains(DESC)) {
            byteParams.add(SafeEncoder.encode(DESC));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
