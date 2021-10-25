package redis.clients.jedis.params;


import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoSearchstoreParam extends GeoSearchParam {
    private final String destination;

    public GeoSearchstoreParam(String dest) {
        this.destination = dest;
    }

    public String getDest() {
        return this.destination;
    }

    public GeoSearchstoreParam storedist() {
        addParam(STOREDIST);
        return this;
    }

    public static GeoSearchstoreParam GeoSearchstoreParam(String dest) { return new GeoSearchstoreParam(dest); }

    @Override
    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<>();
        byteParams.add(SafeEncoder.encode(this.destination));
        Collections.addAll(byteParams, args);
        Collections.addAll(byteParams, super.getByteParams());

        if (contains(STOREDIST)) {
            byteParams.add(SafeEncoder.encode(STOREDIST));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
