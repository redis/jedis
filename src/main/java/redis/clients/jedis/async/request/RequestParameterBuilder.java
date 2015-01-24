package redis.clients.jedis.async.request;

import redis.clients.jedis.BitOP;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;
import redis.clients.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static redis.clients.jedis.Protocol.toByteArray;

public class RequestParameterBuilder {
  public static byte[][] joinParameters(byte[] first, byte[][] rest) {
    byte[][] result = new byte[rest.length + 1][];
    result[0] = first;
    System.arraycopy(rest, 0, result, 1, rest.length);
    return result;
  }

  public static byte[][] joinParameters(String first, String[] rest) {
    String[] result = new String[rest.length + 1];
    result[0] = first;
    System.arraycopy(rest, 0, result, 1, rest.length);
    return SafeEncoder.encodeMany(result);
  }

  public static byte[][] joinParameters(byte[] first, String[] rest) {
    byte[][] restEncoded = SafeEncoder.encodeMany(rest);

    byte[][] result = new byte[restEncoded.length + 1][];
    result[0] = first;
    System.arraycopy(restEncoded, 0, result, 1, restEncoded.length);
    return result;
  }

  public static byte[] convertRangeParameter(double value) {
    if (value == Double.POSITIVE_INFINITY) {
      return "+inf".getBytes();
    } else if (value == Double.NEGATIVE_INFINITY) {
      return "-inf".getBytes();
    }

    return toByteArray(value);
  }

  public static byte[][] buildMultiArgsParameter(byte[] key, byte[]... keys) {
    byte[][] params = new byte[keys.length + 1][];
    params[0] = key;
    System.arraycopy(keys, 0, params, 1, keys.length);

    return params;
  }

  public static byte[][] buildMultiArgsParameter(String key, String... keys) {
    String[] args = new String[keys.length + 1];
    args[0] = key;
    System.arraycopy(keys, 0, args, 1, keys.length);

    return SafeEncoder.encodeMany(args);
  }

  public static byte[][] buildMultiArgsWithArgsLengthParameter(byte[] key, byte[]... keys) {
    final byte[][] params = new byte[keys.length + 2][];
    params[0] = key;
    params[1] = toByteArray(keys.length);
    System.arraycopy(keys, 0, params, 2, keys.length);

    return params;
  }

  public static byte[][] buildSortWithParameter(byte[] key, SortingParams sortingParams) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.addAll(sortingParams.getParams());
    return args.toArray(new byte[args.size()][]);
  }

  public static byte[][] buildSortWithParameter(String key, SortingParams sortingParams) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(SafeEncoder.encode(key));
    args.addAll(sortingParams.getParams());
    return args.toArray(new byte[args.size()][]);
  }

  public static byte[][] buildSortStoreWithParameter(byte[] key, SortingParams sortingParameters,
      byte[] dstkey) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(key);
    args.addAll(sortingParameters.getParams());
    args.add(Protocol.Keyword.STORE.raw);
    args.add(dstkey);
    return args.toArray(new byte[args.size()][]);
  }

  public static byte[][] buildSortedSetStoreWithParameter(byte[] dstkey, ZParams params,
      byte[]... sets) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(dstkey);
    args.add(toByteArray(sets.length));
    for (final byte[] set : sets) {
      args.add(set);
    }
    args.addAll(params.getParams());

    return args.toArray(new byte[args.size()][]);
  }

  public static byte[][] buildSortedSetStoreWithParameter(String dstkey, ZParams params,
      String... sets) {
    final List<byte[]> args = new ArrayList<byte[]>();
    args.add(SafeEncoder.encode(dstkey));
    args.add(toByteArray(sets.length));
    for (final String set : sets) {
      args.add(SafeEncoder.encode(set));
    }
    args.addAll(params.getParams());

    return args.toArray(new byte[args.size()][]);
  }

  public static byte[][] buildSetMultiAddParameter(byte[] key, Map<byte[], byte[]> hash) {
    final List<byte[]> params = new ArrayList<byte[]>();
    params.add(key);

    for (final Map.Entry<byte[], byte[]> entry : hash.entrySet()) {
      params.add(entry.getKey());
      params.add(entry.getValue());
    }

    return params.toArray(new byte[params.size()][]);
  }

  public static byte[][] buildSetMultiAddParameter(String key, Map<String, String> hash) {
    final List<String> params = new ArrayList<String>();
    params.add(key);

    for (final Map.Entry<String, String> entry : hash.entrySet()) {
      params.add(entry.getKey());
      params.add(entry.getValue());
    }

    return SafeEncoder.encodeMany(params.toArray(new String[params.size()]));
  }

  public static byte[][] buildSortedSetMultiAddParameter(byte[] key,
      Map<byte[], Double> scoreMembers) {
    ArrayList<byte[]> args = new ArrayList<byte[]>(scoreMembers.size() * 2 + 1);
    args.add(key);

    for (Map.Entry<byte[], Double> entry : scoreMembers.entrySet()) {
      args.add(toByteArray(entry.getValue()));
      args.add(entry.getKey());
    }

    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    return argsArray;
  }

  public static byte[][] buildSortedSetMultiAddParameter(String key,
      Map<String, Double> scoreMembers) {
    ArrayList<byte[]> args = new ArrayList<byte[]>(scoreMembers.size() * 2 + 1);
    args.add(SafeEncoder.encode(key));

    for (Map.Entry<String, Double> entry : scoreMembers.entrySet()) {
      args.add(toByteArray(entry.getValue()));
      args.add(SafeEncoder.encode(entry.getKey()));
    }

    byte[][] argsArray = new byte[args.size()][];
    args.toArray(argsArray);

    return argsArray;
  }

  public static byte[][] buildBitOpParameter(final BitOP op, final byte[] destKey,
      final byte[]... srcKeys) {
    Protocol.Keyword kw = Protocol.Keyword.AND;
    int len = srcKeys.length;
    switch (op) {
    case AND:
      kw = Protocol.Keyword.AND;
      break;
    case OR:
      kw = Protocol.Keyword.OR;
      break;
    case XOR:
      kw = Protocol.Keyword.XOR;
      break;
    case NOT:
      kw = Protocol.Keyword.NOT;
      len = Math.min(1, len);
      break;
    }

    byte[][] bargs = new byte[len + 2][];
    bargs[0] = kw.raw;
    bargs[1] = destKey;
    for (int i = 0; i < len; ++i) {
      bargs[i + 2] = srcKeys[i];
    }

    return bargs;
  }

  public static byte[][] buildEvalParameter(byte[] script, byte[] keyCount, byte[][] params) {
    final byte[][] allArgs = new byte[params.length + 2][];

    allArgs[0] = script;
    allArgs[1] = keyCount;

    for (int i = 0; i < params.length; i++)
      allArgs[i + 2] = params[i];

    return allArgs;
  }

  public static byte[][] buildEvalParameter(String script, int keyCount, String... params) {
    final byte[][] allArgs = new byte[params.length + 2][];

    allArgs[0] = SafeEncoder.encode(script);
    allArgs[1] = toByteArray(keyCount);

    for (int i = 0; i < params.length; i++)
      allArgs[i + 2] = SafeEncoder.encode(params[i]);

    return allArgs;
  }

  public static byte[][] convertEvalBinaryListArgs(List<byte[]> keys, List<byte[]> args) {
    final int keyCount = keys.size();
    final int argCount = args.size();
    byte[][] params = new byte[keyCount + argCount][];

    for (int i = 0; i < keyCount; i++)
      params[i] = keys.get(i);

    for (int i = 0; i < argCount; i++)
      params[keyCount + i] = args.get(i);

    return params;
  }

  public static String[] convertEvalListArgs(List<String> keys, List<String> args) {
    final int keyCount = keys.size();
    final int argCount = args.size();
    String[] params = new String[keyCount + argCount];

    for (int i = 0; i < keyCount; i++)
      params[i] = keys.get(i);

    for (int i = 0; i < argCount; i++)
      params[keyCount + i] = args.get(i);

    return params;
  }
}
