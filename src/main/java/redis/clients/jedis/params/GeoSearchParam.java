package redis.clients.jedis.params;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoSearchParam extends Params {
    protected static final String FROMMEMBER = "frommember";
    protected static final String FROMLONLAT = "fromlonlat";
    protected static final String BYRADIUS = "byradius";
    protected static final String BYBOX = "bybox";
    protected GeoUnit unit;

    protected static final String WITHCOORD = "withcoord";
    protected static final String WITHDIST = "withdist";
    protected static final String WITHHASH = "withhash";

    protected static final String ASC = "asc";
    protected static final String DESC = "desc";
    protected static final String COUNT = "count";
    protected static final String ANY = "any";

    public GeoSearchParam() { }

    public static GeoSearchParam geoSearchParam() { return new GeoSearchParam(); }

    public GeoSearchParam frommember(String member) {
        addParam(FROMMEMBER, member);
        return this;
    }

    public GeoSearchParam fromlonlat(double longitude, double latitude) {
        addParam(FROMLONLAT, new GeoCoordinate(longitude, latitude));
        return this;
    }

    public GeoSearchParam byradius(double r){
        addParam(BYRADIUS, r);
        return this;
    }

    public GeoSearchParam byradius(double r, GeoUnit unit){
        addParam(BYRADIUS, r);
        this.unit = unit;
        return this;
    }

    public GeoSearchParam bybox(double width, double height){
        addParam(BYBOX, new double[]{width, height});
        return this;
    }

    public GeoSearchParam bybox(double width, double height, GeoUnit unit){
        addParam(BYBOX, new double[]{width, height});
        this.unit = unit;
        return this;
    }

    public GeoSearchParam unit(GeoUnit unit){
        this.unit = unit;
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

        if (contains(FROMMEMBER)) {
            byteParams.add(SafeEncoder.encode(FROMMEMBER));
            byteParams.add(((String) getParam(FROMMEMBER)).getBytes());
        } else if (contains(FROMLONLAT)) {
            byteParams.add(SafeEncoder.encode(FROMLONLAT));
            GeoCoordinate lonlat = getParam(FROMLONLAT);
            byteParams.add(Protocol.toByteArray(lonlat.getLongitude()));
            byteParams.add(Protocol.toByteArray(lonlat.getLatitude()));
        }

        if (contains(BYRADIUS)) {
            byteParams.add(SafeEncoder.encode(BYRADIUS));
            byteParams.add(Protocol.toByteArray((double) getParam(BYRADIUS)));
            if (this.unit != null) {
                byteParams.add(this.unit.raw);
            }
        } else if (contains(BYBOX)) {
            byteParams.add(SafeEncoder.encode(BYBOX));
            double[] box = getParam(BYBOX);
            byteParams.add(Protocol.toByteArray(box[0]));
            byteParams.add(Protocol.toByteArray(box[1]));
            if (this.unit != null) {
                byteParams.add(this.unit.raw);
            }
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
