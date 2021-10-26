package redis.clients.jedis.params;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.Collections;

public class GeoSearchParam extends GeoRadiusParam {
    protected static final String FROMMEMBER = "frommember";
    protected static final String FROMLONLAT = "fromlonlat";
    protected static final String BYRADIUS = "byradius";
    protected static final String BYBOX = "bybox";
    protected GeoUnit unit;

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

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<>();
        Collections.addAll(byteParams, args);
        Collections.addAll(byteParams, getSearchParams());
        Collections.addAll(byteParams, getLabels());
        return byteParams.toArray(new byte[byteParams.size()][]);
    }

    protected byte[][] getSearchParams() {
        ArrayList<byte[]> byteParams = new ArrayList<>();

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

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
