package redis.clients.jedis.modules.json;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class JsonObjects {

  /* A simple class that represents an object in real life */
  @SuppressWarnings("unused")
  public static class IRLObject {

    public String str;
    public boolean bool;

    public IRLObject() {
      this.str = "string";
      this.bool = true;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      final IRLObject other = (IRLObject) obj;
      return Objects.equals(str, other.str)
          && Objects.equals(bool, other.bool);
    }
  }

  @SuppressWarnings("unused")
  public static class FooBarObject {

    public String foo;
    public boolean fooB;
    public int fooI;
    public float fooF;
    public String[] fooArr;

    public FooBarObject() {
      this.foo = "bar";
      this.fooB = true;
      this.fooI = 6574;
      this.fooF = 435.345f;
      this.fooArr = new String[]{"a", "b", "c"};
    }
  }

  public static class Baz {

    String quuz;
    private String grault;
    private String waldo;

    public Baz(final String quuz, final String grault, final String waldo) {
      this.quuz = quuz;
      this.grault = grault;
      this.waldo = waldo;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (getClass() != o.getClass()) {
        return false;
      }
      Baz other = (Baz) o;

      return Objects.equals(quuz, other.quuz)
          && Objects.equals(grault, other.grault)
          && Objects.equals(waldo, other.waldo);
    }
  }

  public static class Qux {

    private String quux;
    private String corge;
    private String garply;
    private Baz baz;

    public Qux(final String quux, final String corge, final String garply, final Baz baz) {
      this.quux = quux;
      this.corge = corge;
      this.garply = garply;
      this.baz = baz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (getClass() != o.getClass()) {
        return false;
      }
      Qux other = (Qux) o;

      return Objects.equals(quux, other.quux)
          && Objects.equals(corge, other.corge)
          && Objects.equals(garply, other.garply)
          && Objects.equals(baz, other.baz);
    }
  }

  public static class Person {
    public String name;
    public int age;
    public String address;
    public String phone;
    public List<String> childrens;

    public Person(String name, int age, String address, String phone, List<String> childrens) {
      this.name = name;
      this.age = age;
      this.address = address;
      this.phone = phone;
      this.childrens = childrens;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      // if (getClass() != o.getClass()) {
      //   return false;
      // }
      Person other = (Person) o;

      return Objects.equals(name, other.name)
            && Objects.equals(age, other.age)
            && Objects.equals(address, other.address)
            && Objects.equals(phone, other.phone)
            && Objects.equals(childrens, other.childrens);
    }
  }

  public static class Tick {
    private final String id;
    private final Instant created;

    public Tick(String id, Instant created) {
      this.id = id;
      this.created = created;
    }

    public String getId() {
      return id;
    }

    public Instant getCreated() {
      return created;
    }
  }
}
