package redis.clients.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

/**
 * Utility class which provides abstraction for iterating over keys in Redis using
 * <strong>SCAN</strong> and other SCAN-like commands.
 * <p>
 * ScanCursor implements {@link Iterable} interface so that iteration over redis keys can be done
 * using {@link Iterator}. Iteration using Iterator is simpler than using SCAN commands and keeping
 * track of current cursor. </> <br>
 * For example implementation without scan cursor could look like this:
 * 
 * <pre>{@code
 * Jedis jedis = ...
 * String currentCursor = ScanParams.SCAN_POINTER_START;
 * ScanParams params = new ScanParams().match("<my_pattern>");
 * do {
 *	ScanResult<String> scanResult = jedis.scan(currentCursor, params);
 * 	for (String key : scanResult.getResult()) {
 * 		//do something with key
 * 	}
 * 	currentCursor = scanResult.getStringCursor();
 * } while (!ScanParams.SCAN_POINTER_START.equals(currentCursor));
 * }
 * </pre>
 * 
 * <br>
 * Implementation using ScanCursor could look like this:
 * <pre>{@code
 * ScanCursor<String> cursor = ScanCursor.builder(jedis)
 *	.pattern("<my_pattern>")
 *	.scan();
 * for (String key : cursor) {
 *	//do something with key
 * }
 * }
 * </pre>
 * 
 * <br>
 * <p>
 * ScanCursor removes some boilerplate end error-prone code when iteration using
 * <strong>SCAN</strong>-like commands are used.
 * </p>
 * <p>
 * NOTE: order of iteration is undefined documented on https://redis.io/commands/scan for all
 * SCAN-like commands
 * </p>
 * <p>
 * NOTE: when iteration is performed, fetching of next keys is done lazy, as needed to continue
 * iteration.
 * </p>
 * <p>
 * NOTE: it is possible to use same Jedis instance while performing iteration. But instance of Jedis
 * must not be left in state in the middle of unfinished pipeline nor transaction.
 * </p>
 * <br>
 * Allowed:
 * 
 * <pre>{@code
 * for (String key : cursor) {
 *	//example operation which uses same Jedis
 *	//instance as ScanCursor uses
 *	jedis.incr(key);
 * }
 * }
 * </pre>
 * 
 * Not allowed:
 * <pre>{@code
 * Pipeline pipeline = jedis.pipelined();
 * for (String key : cursor) {
 *	//example operation which uses pipeline on
 *	//same Jedis instance as ScanCursor uses
 *	pipeline.incr(key);	//example operation
 * }
 * p.sync();
 * }
 * </pre>
 * @author Antonio Tomac <antonio.tomac@mediatoolkit.com>
 * @param <T> type of elements on which ScanCursor iterates
 */
public class ScanCursor<T> implements Iterable<T> {

  private final ScanOperation<T> scanOperation;

  private static abstract class ScanOperation<T> {

    protected final Jedis jedis;
    protected final ScanParams params;

    ScanOperation(Jedis jedis, ScanParams params) {
      this.jedis = jedis;
      this.params = params;
    }

    abstract ScanResult<T> nextScan(ScanResult<T> currentScanResult);
  }

  private static class KeyScanOperation extends ScanOperation<String> {

    KeyScanOperation(Jedis jedis, ScanParams params) {
      super(jedis, params);
    }

    @Override
    ScanResult<String> nextScan(ScanResult<String> currentScanResult) {
      return jedis.scan(currentScanResult.getCursor(), params);
    }

  }

  private static class BinKeyScanOperation extends ScanOperation<byte[]> {

    BinKeyScanOperation(Jedis jedis, ScanParams params) {
      super(jedis, params);
    }

    @Override
    ScanResult<byte[]> nextScan(ScanResult<byte[]> currentScanResult) {
      return jedis.scan(currentScanResult.getCursorAsBytes(), params);
    }

  }

  private static class SScanOperation extends ScanOperation<String> {

    private final String sSetKey;

    SScanOperation(Jedis jedis, ScanParams params, String sSetKey) {
      super(jedis, params);
      this.sSetKey = sSetKey;
    }

    @Override
    ScanResult<String> nextScan(ScanResult<String> currentScanResult) {
      return jedis.sscan(sSetKey, currentScanResult.getCursor(), params);
    }

  }

  private static class BinSScanOperation extends ScanOperation<byte[]> {

    private final byte[] sSetKey;

    BinSScanOperation(Jedis jedis, ScanParams params, byte[] sSetKey) {
      super(jedis, params);
      this.sSetKey = sSetKey;
    }

    @Override
    ScanResult<byte[]> nextScan(ScanResult<byte[]> currentScanResult) {
      return jedis.sscan(sSetKey, currentScanResult.getCursorAsBytes(), params);
    }

  }

  private static class ZScanOperation extends ScanOperation<Tuple> {

    private final String zSetKey;

    ZScanOperation(Jedis jedis, ScanParams params, String zSetKey) {
      super(jedis, params);
      this.zSetKey = zSetKey;
    }

    @Override
    ScanResult<Tuple> nextScan(ScanResult<Tuple> currentScanResult) {
      return jedis.zscan(zSetKey, currentScanResult.getCursor(), params);
    }

  }

  private static class HScanOperation extends ScanOperation<Entry<String, String>> {

    private final String hKey;

    HScanOperation(Jedis jedis, ScanParams params, String hKey) {
      super(jedis, params);
      this.hKey = hKey;
    }

    @Override
    ScanResult<Entry<String, String>> nextScan(ScanResult<Entry<String, String>> currentScanResult) {
      return jedis.hscan(hKey, currentScanResult.getCursor(), params);
    }

  }

  private static class BinHScanOperation extends ScanOperation<Entry<byte[], byte[]>> {

    private final byte[] hKey;

    BinHScanOperation(Jedis jedis, ScanParams params, byte[] hKey) {
      super(jedis, params);
      this.hKey = hKey;
    }

    @Override
    ScanResult<Entry<byte[], byte[]>> nextScan(ScanResult<Entry<byte[], byte[]>> currentScanResult) {
      return jedis.hscan(hKey, currentScanResult.getCursorAsBytes(), params);
    }

  }

  /**
   * @param jedis A {@link Jedis} instance to be used for sending scan commands
   * @return new builder for ScanCursor
   */
  public static CursorBuilder builder(Jedis jedis) {
    return new CursorBuilder(jedis);
  }

  public static final class CursorBuilder {

    private final Jedis jedis;
    private ScanParams params;

    private CursorBuilder(Jedis jedis) {
      this.jedis = jedis;
      params = new ScanParams();
    }

    /**
     * @param pattern to use for matching keys, if not set than all keys will match
     * @return {@code this} builder
     */
    public CursorBuilder pattern(String pattern) {
      params.match(pattern);
      return this;
    }

    /**
     * @param pattern to use for matching keys, if not set than all keys will match
     * @return {@code this} builder
     */
    public CursorBuilder pattern(byte[] pattern) {
      params.match(pattern);
      return this;
    }

    /**
     * @param count to limit maximum number of elements per each round-trip to redis. If not set,
     *          than redis default (10) size is used
     * @return {@code this} builder
     */
    public CursorBuilder count(int count) {
      params.count(count);
      return this;
    }

    /**
     * @param params to be used when executing scan commands
     * @return {@code this} builder
     */
    public CursorBuilder scanParams(ScanParams params) {
      this.params = params;
      return this;
    }

    /**
     * Builds a ScanCursor over keys in redis which uses <strong>SCAN</strong> command.
     * @return a new instance of ScanCursor
     */
    public ScanCursor<String> scan() {
      return new ScanCursor<>(new KeyScanOperation(jedis, params));
    }

    /**
     * Builds a ScanCursor over keys in redis which uses <strong>SCAN</strong> command.
     * @return a new instance of ScanCursor
     */
    public ScanCursor<byte[]> scanBin() {
      return new ScanCursor<>(new BinKeyScanOperation(jedis, params));
    }

    /**
     * Builds a ScanCursor over members of SET which uses <strong>SSCAN</strong> command.
     * @param sSetKey of SET target for iteration
     * @return a new instance of ScanCursor
     */
    public ScanCursor<String> sScan(String sSetKey) {
      return new ScanCursor<>(new SScanOperation(jedis, params, sSetKey));
    }

    /**
     * Builds a ScanCursor over members of SET which uses <strong>SSCAN</strong> command.
     * @param sSetKey of SET target for iteration
     * @return a new instance of ScanCursor
     */
    public ScanCursor<byte[]> sScanBin(byte[] sSetKey) {
      return new ScanCursor<>(new BinSScanOperation(jedis, params, sSetKey));
    }

    /**
     * Builds a ScanCursor over members of ZSET which uses <strong>ZSCAN</strong> command.
     * @param zSetKey of ZSET target for iteration
     * @return a new instance of ScanCursor
     */
    public ScanCursor<Tuple> zScan(String zSetKey) {
      return new ScanCursor<>(new ZScanOperation(jedis, params, zSetKey));
    }

    /**
     * Builds a ScanCursor over entries of HASH which uses <strong>HSCAN</strong> command.
     * @param hashKey of HASH target for iteration
     * @return a new instance of ScanCursor
     */
    public ScanCursor<Entry<String, String>> hScan(String hashKey) {
      return new ScanCursor<>(new HScanOperation(jedis, params, hashKey));
    }

    /**
     * Builds a ScanCursor over entries of HASH which uses <strong>HSCAN</strong> command.
     * @param hashKey of HASH target for iteration
     * @return a new instance of ScanCursor
     */
    public ScanCursor<Entry<byte[], byte[]>> hScanBin(byte[] hashKey) {
      return new ScanCursor<>(new BinHScanOperation(jedis, params, hashKey));
    }

  }

  private ScanCursor(ScanOperation<T> scanOperation) {
    this.scanOperation = scanOperation;
  }

  @Override
  public Iterator<T> iterator() {
    return new CursorIterator();
  }

  /**
   * Utility method which makes full iteration and stores all elements into a Set
   * @return new Set consisting of all iterated elements
   */
  public Set<T> toSet() {
    Set<T> set = new HashSet<>();
    Iterator<T> it = iterator();
    while (it.hasNext()) {
      T next = it.next();
      set.add(next);
    }
    return set;
  }

  private static final ScanResult<?> EMPTY_SCAN_RESULT = new ScanResult<>(
      ScanParams.SCAN_POINTER_START_BINARY, Collections.emptyList());

  private class CursorIterator implements Iterator<T> {

    private Iterator<T> currentBatchIt;
    private ScanResult<T> currentScanResult;
    private boolean reachedLastBatch;

    @SuppressWarnings("unchecked")
    public CursorIterator() {
      this.currentScanResult = (ScanResult<T>) EMPTY_SCAN_RESULT;
      this.currentBatchIt = currentScanResult.getResult().iterator();
      this.reachedLastBatch = false;
    }

    private void ensureMore() {
      if (reachedLastBatch || currentBatchIt.hasNext()) {
        return;
      }
      boolean cursorCompleted;
      do {
        currentScanResult = scanOperation.nextScan(currentScanResult);
        currentBatchIt = currentScanResult.getResult().iterator();
        cursorCompleted = currentScanResult.isCompleteIteration();
      } while (!currentBatchIt.hasNext() && !cursorCompleted);
      if (cursorCompleted) {
        reachedLastBatch = true;
      }
    }

    @Override
    public boolean hasNext() {
      ensureMore();
      return currentBatchIt.hasNext();
    }

    @Override
    public T next() {
      ensureMore();
      return currentBatchIt.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removal on interation is not supported");
    }

  }

}