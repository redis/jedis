package redis.clients.jedis.params;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.GeoUnit;
import static redis.clients.jedis.Protocol.Keyword.ANY;
import static redis.clients.jedis.Protocol.Keyword.ASC;
import static redis.clients.jedis.Protocol.Keyword.BYBOX;
import static redis.clients.jedis.Protocol.Keyword.BYRADIUS;
import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.DESC;
import static redis.clients.jedis.Protocol.Keyword.WITHCOORD;
import static redis.clients.jedis.Protocol.Keyword.WITHDIST;
import static redis.clients.jedis.Protocol.Keyword.WITHHASH;
import static redis.clients.jedis.Protocol.Keyword.FROMMEMBER;
import static redis.clients.jedis.Protocol.Keyword.FROMLONLAT;


public class GeoSearchParam implements IParams {
    private boolean frommember = false;
    private boolean fromlonlat = false;
    private String member;
    private GeoCoordinate lonlat;

    private boolean byradius = false;
    private boolean bybox = false;
    private double radius;
    private double width;
    private double height;
    private GeoUnit unit;

    private boolean withCoord = false;
    private boolean withDist = false;
    private boolean withHash = false;

    private Integer count = null;
    private boolean any = false;
    private boolean asc = false;
    private boolean desc = false;

    public GeoSearchParam() { }

    public static GeoSearchParam geoSearchParam() { return new GeoSearchParam(); }

    public GeoSearchParam fromMember(String member) {
        this.frommember = true;
        this.member = member;
        return this;
    }

    public GeoSearchParam fromLonLat(double longitude, double latitude) {
        this.fromlonlat = true;
        this.lonlat = new GeoCoordinate(longitude, latitude);
        return this;
    }

    public GeoSearchParam fromLonLat(GeoCoordinate coord) {
        this.fromlonlat = true;
        this.lonlat = coord;
        return this;
    }


    public GeoSearchParam byRadius(double r, GeoUnit unit){
        this.byradius = true;
        this.radius = r;
        this.unit = unit;
        return this;
    }

    public GeoSearchParam byBox(double width, double height, GeoUnit unit){
        this.bybox = true;
        this.width = width;
        this.height = height;
        this.unit = unit;
        return this;
    }

    public GeoSearchParam withCoord() {
        withCoord = true;
        return this;
    }

    public GeoSearchParam withDist() {
        withDist = true;
        return this;
    }

    public GeoSearchParam withHash() {
        withHash = true;
        return this;
    }

    public GeoSearchParam sortAscending() {
        asc = true;
        return this;
    }

    public GeoSearchParam sortDescending() {
        desc = true;
        return this;
    }

    public GeoSearchParam count(int count) {
        if (count > 0) {
            this.count = count;
        }
        return this;
    }

    public GeoSearchParam count(int count, boolean any) {
        if (count > 0) {
            this.count = count;

            if (any) {
                this.any = true;
            }
        }
        return this;
    }

    @Override
    public void addParams(CommandArguments args) {
        if (this.frommember) {
            args.add(FROMMEMBER);
            args.add(this.member);
        } else if (this.fromlonlat) {
            args.add(FROMLONLAT);
            args.add(lonlat.getLongitude());
            args.add(lonlat.getLatitude());
        }

        if (this.byradius) {
            args.add(BYRADIUS);
            args.add(this.radius);
        } else if (this.bybox) {
            args.add(BYBOX);
            args.add(this.width);
            args.add(this.height);
        }
        args.add(this.unit);

        if (withCoord) {
            args.add(WITHCOORD);
        }
        if (withDist) {
            args.add(WITHDIST);
        }
        if (withHash) {
            args.add(WITHHASH);
        }

        if (count != null) {
            args.add(COUNT).add(count);
            if (any) {
                args.add(ANY);
            }
        }

        if (asc) {
            args.add(ASC);
        } else if (desc) {
            args.add(DESC);
        }
    }
}
