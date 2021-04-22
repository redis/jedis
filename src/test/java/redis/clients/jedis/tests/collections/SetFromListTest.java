package redis.clients.jedis.tests.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    List<String> list = a2z();

    Set<String> cut = setFromList(list);

    // ordering guarantee
    int i = 0;
    for (String x : cut) {
      assertEquals(list.get(i++), x);
    }
  }

  @Test
  public void equals() throws Exception {

    List<String> list = a2z();

    Set<String> hashSet = new HashSet<String>(list);

    Set<String> cut = setFromList(list);

    assertEquals(hashSet, cut);
  }

  @Test
  public void serialize() throws Exception {

    Set<String> set = setFromList(a2z());

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
    objOut.writeObject(set);

    ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
    ObjectInputStream objIn = new ObjectInputStream(byteIn);

    Set<String> setRead = (Set<String>) objIn.readObject();

    assertEquals(set, setRead);
  }

  private List<String> a2z() {
    List<String> list = new ArrayList<String>();

    for (int i = 'a'; i <= 'z'; i++) {
      list.add(String.valueOf((char) i));
    }

    Collections.shuffle(list);
    return list;
  }
}
