package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import redis.clients.jedis.util.SafeEncoder;

public class GeoRadiusStoreParam extends Params {
    private static final String STORE = "store";
    private static final String STOREDIST = "storedist";

    public GeoRadiusStoreParam() {
    }

    public static GeoRadiusStoreParam geoRadiusStoreParam() {
        return new GeoRadiusStoreParam();
    }

    public GeoRadiusStoreParam store(String key) {
        if (key != null) {
            addParam(STORE, key);
        }
        return this;
    }

    public GeoRadiusStoreParam storeDist(String key) {
        if (key != null) {
            addParam(STOREDIST, key);
        }
        return this;
    }

    /**
     * NOTICE: In Redis, if STOREDIST exists, store will be ignored.
     * refer: https://github.com/antirez/redis/blob/6.0/src/geo.c#L649
     *
     * @return STORE or STOREDIST
     */
    public byte[] getOption() {
        if (contains(STOREDIST)) {
            return SafeEncoder.encode(STOREDIST);
        }

        if (contains(STORE)) {
            return SafeEncoder.encode(STORE);
        }

        throw new IllegalArgumentException(this.getClass().getSimpleName()
            + " must has store or storedist option");
    }

    public byte[] getKey() {
        if (contains(STOREDIST)) {
            return SafeEncoder.encode((String)getParam(STOREDIST));
        }

        if (contains(STORE)) {
            return SafeEncoder.encode((String)getParam(STORE));
        }

        throw new IllegalArgumentException(this.getClass().getSimpleName()
            + " must has store or storedist key");
    }

    public String[] getStringKeys(String key) {
        List<String> keys = new LinkedList<>();
        keys.add(key);

        if (contains(STORE)) {
            keys.add((String)getParam(STORE));
        }

        if (contains(STOREDIST)) {
            keys.add((String)getParam(STOREDIST));
        }
        return keys.toArray(new String[keys.size()]);
    }

    public byte[][] getByteKeys(byte[] key) {
        List<byte[]> keys = new LinkedList<>();
        keys.add(key);

        if (contains(STORE)) {
            keys.add(SafeEncoder.encode((String)getParam(STORE)));
        }

        if (contains(STOREDIST)) {
            keys.add(SafeEncoder.encode((String)getParam(STOREDIST)));
        }
        return keys.toArray(new byte[keys.size()][]);
    }

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        for (byte[] arg : args) {
            byteParams.add(arg);
        }

        if (contains(STORE)) {
            byteParams.add(SafeEncoder.encode(STORE));
            byteParams.add(SafeEncoder.encode((String)getParam(STORE)));
        }

        if (contains(STOREDIST)) {
            byteParams.add(SafeEncoder.encode(STOREDIST));
            byteParams.add(SafeEncoder.encode((String)getParam(STOREDIST)));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
