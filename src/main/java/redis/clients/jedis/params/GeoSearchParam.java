package redis.clients.jedis.params;


import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoUnit;

public class GeoSearchParam extends GeoRadiusParam {

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
}
