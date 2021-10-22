package redis.clients.jedis.params;

import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoSearchParam extends Params {
    private static final String FROMMEMBER = "frommember";
    private static final String FROMLONLAT = "fromlonlat";
    private static final String BYRADIUS = "byradius";
    private static final String BYBOX = "bybox";

    private static final String WITHCOORD = "withcoord";
    private static final String WITHDIST = "withdist";
    private static final String WITHHASH = "withhash";

    private static final String ASC = "asc";
    private static final String DESC = "desc";
    private static final String COUNT = "count";
    private static final String ANY = "any";

    public GeoSearchParam() {
    }

    public static GeoSearchParam geoSearchParam() { return new GeoSearchParam(); }

    public GeoSearchParam byRadius(double radius, GeoUnit unit) {
        addParam(BYRADIUS, radius + " " + unit.toString());
        return this;
    }

    public GeoSearchParam byBox(double width, double height, GeoUnit unit) {
        addParam(BYBOX, width + " " + height + " " + unit.toString());
        return this;
    }

    public GeoSearchParam withCoord() {
        addParam(WITHCOORD);
        return this;
    }

    public GeoSearchParam withDist() {
        addParam(WITHDIST);
        return this;
    }

    public GeoSearchParam withHash() {
        addParam(WITHHASH);
        return this;
    }

    public GeoSearchParam sortAscending() {
        addParam(ASC);
        return this;
    }

    public GeoSearchParam sortDescending() {
        addParam(DESC);
        return this;
    }

    public GeoSearchParam count(int count) {
        if (count > 0) {
            addParam(COUNT, count);
        }
        return this;
    }

    public GeoSearchParam count(int count, boolean any) {
        if (count > 0) {
            addParam(COUNT, count);
            if (any) {
                addParam(ANY);
            }
        }
        return this;
    }

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
