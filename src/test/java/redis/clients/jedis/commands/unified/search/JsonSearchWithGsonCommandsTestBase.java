package redis.clients.jedis.commands.unified.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static redis.clients.jedis.util.AssertUtil.assertOK;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.search.*;

/**
 * Base test class for JSON Search with Gson serialization using the UnifiedJedis pattern.
 */
@Tag("integration")
@Tag("search")
public abstract class JsonSearchWithGsonCommandsTestBase extends UnifiedJedisCommandsTestBase {

  private static final String INDEX = "gson-index";

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public JsonSearchWithGsonCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected void clearData() {
    // Drop search index if it exists
    try {
      jedis.ftDropIndex(INDEX);
    } catch (Exception e) {
      // Index might not exist, ignore
    }
    // Then call parent's clearData which does flushAll
    super.clearData();
  }

  static class Account {

    String name;
    String phone;
    Integer age;

    public Account(String name, String phone, Integer age) {
      this.name = name;
      this.phone = phone;
      this.age = age;
    }
  }

  @Test
  public void returnNullField() {
    Gson nullGson = new GsonBuilder().serializeNulls().create();

    assertOK(jedis.ftCreate(INDEX, FTCreateParams.createParams().on(IndexDataType.JSON),
      redis.clients.jedis.search.schemafields.TextField.of(FieldName.of("$.name").as("name")),
      redis.clients.jedis.search.schemafields.TextField.of(FieldName.of("$.phone").as("phone")),
      redis.clients.jedis.search.schemafields.NumericField.of(FieldName.of("$.age").as("age"))));

    Account object = new Account("Jane", null, null);
    String jsonString = nullGson.toJson(object);
    jedis.jsonSet("account:2", jsonString);

    SearchResult sr = jedis.ftSearch(INDEX, "*",
      FTSearchParams.searchParams().returnFields("name", "phone", "age"));
    assertEquals(1, sr.getTotalResults());
    Document doc = sr.getDocuments().get(0);
    assertEquals("Jane", doc.get("name"));
    assertNull(doc.get("phone"));
    assertNull(doc.get("age"));

    sr = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnFields("name"));
    assertEquals(1, sr.getTotalResults());
    doc = sr.getDocuments().get(0);
    assertEquals("Jane", doc.get("name"));

    sr = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnFields("phone"));
    assertEquals(1, sr.getTotalResults());
    doc = sr.getDocuments().get(0);
    assertNull(doc.get("phone"));

    sr = jedis.ftSearch(INDEX, "*", FTSearchParams.searchParams().returnFields("age"));
    assertEquals(1, sr.getTotalResults());
    doc = sr.getDocuments().get(0);
    assertNull(doc.get("age"));
  }
}
