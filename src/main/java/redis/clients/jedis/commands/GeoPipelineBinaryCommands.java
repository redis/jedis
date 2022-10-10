package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.resps.GeoRadiusResponse;

public interface GeoPipelineBinaryCommands {

  Response<Long> geoadd(byte[] key, double longitude, double latitude, byte[] member);

  Response<Long> geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap);

  Response<Long> geoadd(byte[] key, GeoAddParams params, Map<byte[], GeoCoordinate> memberCoordinateMap);

  Response<Double> geodist(byte[] key, byte[] member1, byte[] member2);

  Response<Double> geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit);

  Response<List<byte[]>> geohash(byte[] key, byte[]... members);

  Response<List<GeoCoordinate>> geopos(byte[] key, byte[]... members);

  Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusReadonly(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadius(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusReadonly(byte[] key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(byte[] key, byte[] member, double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(byte[] key, byte[] member, double radius,
      GeoUnit unit, GeoRadiusParam param);

  Response<Long> georadiusStore(byte[] key, double longitude, double latitude, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  Response<Long> georadiusByMemberStore(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param, GeoRadiusStoreParam storeParam);

  Response<List<GeoRadiusResponse>> geosearch(byte[] key, byte[] member, double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> geosearch(byte[] key, GeoCoordinate coord, double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> geosearch(byte[] key, byte[] member, double width, double height, GeoUnit unit);

  Response<List<GeoRadiusResponse>> geosearch(byte[] key, GeoCoordinate coord, double width, double height, GeoUnit unit);

  Response<List<GeoRadiusResponse>> geosearch(byte[] key, GeoSearchParam params);

  Response<Long> geosearchStore(byte[] dest, byte[] src, byte[] member, double radius, GeoUnit unit);

  Response<Long> geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double radius, GeoUnit unit);

  Response<Long> geosearchStore(byte[] dest, byte[] src, byte[] member, double width, double height, GeoUnit unit);

  Response<Long> geosearchStore(byte[] dest, byte[] src, GeoCoordinate coord, double width, double height, GeoUnit unit);

  Response<Long> geosearchStore(byte[] dest, byte[] src, GeoSearchParam params);

  Response<Long> geosearchStoreStoreDist(byte[] dest, byte[] src, GeoSearchParam params);
}
