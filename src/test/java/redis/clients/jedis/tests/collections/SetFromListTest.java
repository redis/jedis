package redis.clients.jedis.tests.collections;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class SetFromListTest {

  private static Method method;

  @BeforeClass
  public static void beforeClass() throws Exception {
    Class<?> clazz = Class.forName("redis.clients.jedis.BinaryJedis$SetFromList");
    method = clazz.getDeclaredMethod("of", List.class);
    method.setAccessible(true);
  }

  /**
   * Instantiate SetFromList class by reflection because it is protected static inner class of
   * BinaryJedis.
   */
  @SuppressWarnings("unchecked")
  private <E> Set<E> setFromList(List<E> list) throws Exception {
    return (Set<E>) method.invoke(null, list);
  }

  @Test
  public void setOperations() throws Exception {

    // add
    Set<String> cut = setFromList(new ArrayList<String>());
    cut.add("A");
    cut.add("B");
    cut.add("A");

    assertEquals(2, cut.size());

    // remove
    cut.remove("A");
    assertEquals(1, cut.size());

    cut.remove("C");
    assertEquals(1, cut.size());

    // contains
    assertTrue(cut.contains("B"));
    assertFalse(cut.contains("A"));

    cut.add("C");
    cut.add("D");

    // containsAll
    assertTrue(cut.containsAll(cut));

    // retainAll
    cut.retainAll(Arrays.asList("C", "D"));
    assertEquals(2, cut.size());
    assertTrue(cut.contains("C"));
    assertTrue(cut.contains("D"));

    // removeAll
    cut.removeAll(Arrays.asList("C"));
    assertEquals(1, cut.size());
    assertTrue(cut.contains("D"));

    // clear
    cut.clear();
    assertTrue(cut.isEmpty());
  }

  @Test
  public void iteration() throws Exception {

    List<String> list = new ArrayList<String>();

    for (int i = 'a'; i <= 'z'; i++) {
      list.add(String.valueOf((char) i));
    }

    Set<String> cut = setFromList(list);

    // ordering guarantee
    int i = 0;
    for (String x : cut) {
      assertEquals(list.get(i++), x);
    }
  }

  @Test
  public void equals() throws Exception {

    Set<String> hashSet = new HashSet<String>();

    for (int i = 'a'; i <= 'z'; i++) {
      hashSet.add(String.valueOf((char) i));
    }

    Set<String> cut = setFromList(new ArrayList<String>(hashSet));

    assertTrue(hashSet.equals(cut));
    assertTrue(cut.equals(hashSet));

    // equals with null
    assertFalse(cut.equals(null));

    // equals with other types
    assertFalse(cut.equals(new ArrayList<String>()));
  }
}
