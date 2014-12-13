package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.ArrayList;
import java.util.List;

public class AsyncSortingCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, '1' };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, '2' };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, '3' };
  final byte[] bbar10 = { 0x05, 0x06, 0x07, 0x08, '1', '0' };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };
  final byte[] bcar1 = { 0x0A, 0x0B, 0x0C, 0x0D, '1' };
  final byte[] bcar2 = { 0x0A, 0x0B, 0x0C, 0x0D, '2' };
  final byte[] bcar10 = { 0x0A, 0x0B, 0x0C, 0x0D, '1', '0' };
  final byte[] bcarstar = { 0x0A, 0x0B, 0x0C, 0x0D, '*' };
  final byte[] b1 = { '1' };
  final byte[] b2 = { '2' };
  final byte[] b3 = { '3' };
  final byte[] b10 = { '1', '0' };

  @Test
  public void sort() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo");
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("1");
    expected.add("2");
    expected.add("3");

    assertEquals(expected, result);

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b1);
    bexpected.add(b2);
    bexpected.add(b3);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortBy() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");

    CommandWithWaiting.set(asyncJedis, "bar1", "3");
    CommandWithWaiting.set(asyncJedis, "bar2", "2");
    CommandWithWaiting.set(asyncJedis, "bar3", "1");

    SortingParams sp = new SortingParams();
    sp.by("bar*");

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo", sp);
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("2");
    expected.add("1");

    assertEquals(expected, result);

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);

    CommandWithWaiting.set(asyncJedis, bbar1, b3);
    CommandWithWaiting.set(asyncJedis, bbar2, b2);
    CommandWithWaiting.set(asyncJedis, bbar3, b1);

    SortingParams bsp = new SortingParams();
    bsp.by(bbarstar);

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bsp);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(b2);
    bexpected.add(b1);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortDesc() {
    // blocking API
    CommandWithWaiting.lpush(asyncJedis, "foo", "3");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");

    SortingParams sp = new SortingParams();
    sp.desc();

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo", sp);
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("3");
    expected.add("2");
    expected.add("1");

    assertEquals(expected, result);

    // Binary
    // blocking API
    CommandWithWaiting.lpush(asyncJedis, bfoo, b3);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);

    SortingParams bsp = new SortingParams();
    bsp.desc();

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bsp);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b3);
    bexpected.add(b2);
    bexpected.add(b1);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortLimit() {
    for (int n = 10; n > 0; n--) {
      CommandWithWaiting.lpush(asyncJedis, "foo", String.valueOf(n));
    }

    SortingParams sp = new SortingParams();
    sp.limit(0, 3);

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo", sp);
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("1");
    expected.add("2");
    expected.add("3");

    assertEquals(expected, result);

    // Binary
    jedis.rpush(bfoo, new byte[] { (byte) '4' });
    jedis.rpush(bfoo, new byte[] { (byte) '3' });
    jedis.rpush(bfoo, new byte[] { (byte) '2' });
    jedis.rpush(bfoo, new byte[] { (byte) '1' });

    SortingParams bsp = new SortingParams();
    bsp.limit(0, 3);

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bsp);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b1);
    bexpected.add(b2);
    bexpected.add(b3);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortAlpha() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "10");

    SortingParams sp = new SortingParams();
    sp.alpha();

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo", sp);
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("1");
    expected.add("10");
    expected.add("2");

    assertEquals(expected, result);

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b10);

    SortingParams bsp = new SortingParams();
    bsp.alpha();

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bsp);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b1);
    bexpected.add(b10);
    bexpected.add(b2);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortGet() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "10");

    CommandWithWaiting.set(asyncJedis, "bar1", "bar1");
    CommandWithWaiting.set(asyncJedis, "bar2", "bar2");
    CommandWithWaiting.set(asyncJedis, "bar10", "bar10");

    CommandWithWaiting.set(asyncJedis, "car1", "car1");
    CommandWithWaiting.set(asyncJedis, "car2", "car2");
    CommandWithWaiting.set(asyncJedis, "car10", "car10");

    SortingParams sp = new SortingParams();
    sp.get("car*", "bar*");

    asyncJedis.sort(STRING_LIST_CALLBACK.withReset(), "foo", sp);
    List<String> result = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("car1");
    expected.add("bar1");
    expected.add("car2");
    expected.add("bar2");
    expected.add("car10");
    expected.add("bar10");

    assertEquals(expected, result);

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b10);

    CommandWithWaiting.set(asyncJedis, bbar1, bbar1);
    CommandWithWaiting.set(asyncJedis, bbar2, bbar2);
    CommandWithWaiting.set(asyncJedis, bbar10, bbar10);

    CommandWithWaiting.set(asyncJedis, bcar1, bcar1);
    CommandWithWaiting.set(asyncJedis, bcar2, bcar2);
    CommandWithWaiting.set(asyncJedis, bcar10, bcar10);

    SortingParams bsp = new SortingParams();
    bsp.get(bcarstar, bbarstar);

    asyncJedis.sort(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bsp);
    List<byte[]> bresult = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bcar1);
    bexpected.add(bbar1);
    bexpected.add(bcar2);
    bexpected.add(bbar2);
    bexpected.add(bcar10);
    bexpected.add(bbar10);

    assertEquals(bexpected, bresult);
  }

  @Test
  public void sortStore() {
    CommandWithWaiting.lpush(asyncJedis, "foo", "1");
    CommandWithWaiting.lpush(asyncJedis, "foo", "2");
    CommandWithWaiting.lpush(asyncJedis, "foo", "10");

    asyncJedis.sort(LONG_CALLBACK.withReset(), "foo", "result");
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    List<String> expected = new ArrayList<String>();
    expected.add("1");
    expected.add("2");
    expected.add("10");

    assertEquals(3, result);
    asyncJedis.lrange(STRING_LIST_CALLBACK.withReset(), "result", 0, 1000);
    assertEquals(expected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.lpush(asyncJedis, bfoo, b1);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b2);
    CommandWithWaiting.lpush(asyncJedis, bfoo, b10);

    byte[] bkresult = new byte[] { 0X09, 0x0A, 0x0B, 0x0C };

    asyncJedis.sort(LONG_CALLBACK.withReset(), bfoo, bkresult);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(b1);
    bexpected.add(b2);
    bexpected.add(b10);

    assertEquals(3, bresult);
    asyncJedis.lrange(BYTE_ARRAY_LIST_CALLBACK.withReset(), bkresult, 0, 1000);
    assertEquals(bexpected, BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000));
  }

}