package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.aggr.Reducers;

/**
 * Tests related to <a href="https://redis.io/commands/?group=search">Search and query</a> commands.
 */
public class CommandObjectsSearchAndQueryCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsSearchAndQueryCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testFtSearchHash() {
    String indexName = "booksIndex";

    IndexDefinition indexDefinition =
        new IndexDefinition(IndexDefinition.Type.HASH).setPrefixes("books:");

    IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(indexDefinition);

    Schema schema = new Schema()
        .addField(new Schema.Field("title", Schema.FieldType.TEXT))
        .addField(new Schema.Field("price", Schema.FieldType.NUMERIC));

    String create = exec(commandObjects.ftCreate(indexName, indexOptions, schema));
    assertThat(create, equalTo("OK"));

    // Set individual fields.
    String book1000 = "books:1000";

    Long hset = exec(commandObjects.hsetObject(book1000, "title", "Redis in Action"));
    assertThat(hset, equalTo(1L));

    hset = exec(commandObjects.hsetObject(book1000, "price", 17.99));
    assertThat(hset, equalTo(1L));

    hset = exec(commandObjects.hsetObject(book1000, "author", "John Doe"));
    assertThat(hset, equalTo(1L));

    // Set multiple fields.
    Map<String, Object> hash = new HashMap<>();
    hash.put("title", "Redis Essentials");
    hash.put("price", 19.99);
    hash.put("author", "Jane Doe");
    String book1200 = "books:1200";

    Long hsetMultiple = exec(commandObjects.hsetObject(book1200, hash));
    assertThat(hsetMultiple, equalTo(3L));

    // Text search.
    SearchResult search = exec(commandObjects.ftSearch(indexName, "Action"));

    assertThat(search.getTotalResults(), equalTo(1L));
    assertThat(search.getDocuments(), hasSize(1));

    Document document = search.getDocuments().get(0);
    assertThat(document.getId(), equalTo(book1000));
    assertThat(document.get("title"), equalTo("Redis in Action"));
    assertThat(document.get("price"), equalTo("17.99"));
    assertThat(document.get("author"), equalTo("John Doe"));

    // Price range search.
    SearchResult searchByPrice = exec(commandObjects.ftSearch(indexName, "@price:[19 +inf]"));

    assertThat(searchByPrice.getTotalResults(), equalTo(1L));
    assertThat(searchByPrice.getDocuments(), hasSize(1));

    Document documentByPrice = searchByPrice.getDocuments().get(0);
    assertThat(documentByPrice.getId(), equalTo(book1200));
    assertThat(documentByPrice.get("title"), equalTo("Redis Essentials"));
    assertThat(documentByPrice.get("price"), equalTo("19.99"));
    assertThat(documentByPrice.get("author"), equalTo("Jane Doe"));

    // Price range search with sorting.
    FTSearchParams ftSearchParams = new FTSearchParams().sortBy("price", SortingOrder.DESC);
    SearchResult searchByPriceWithParams = exec(commandObjects.ftSearch(indexName, "@price:[10 20]", ftSearchParams));

    assertThat(searchByPriceWithParams.getTotalResults(), equalTo(2L));
    assertThat(searchByPriceWithParams.getDocuments(), hasSize(2));
    assertThat(searchByPriceWithParams.getDocuments().stream().map(Document::getId).collect(Collectors.toList()),
        contains(book1200, book1000));

    Query query = new Query()
        .addFilter(new Query.NumericFilter("price", 19.0, 20.0))
        .returnFields("price", "title");
    SearchResult searchByPriceWithQuery = exec(commandObjects.ftSearch(indexName, query));

    assertThat(searchByPriceWithQuery.getTotalResults(), equalTo(1L));
    assertThat(searchByPriceWithQuery.getDocuments(), hasSize(1));

    Document documentByPriceWithQuery = searchByPriceWithQuery.getDocuments().get(0);
    assertThat(documentByPriceWithQuery.getId(), equalTo(book1200));
    assertThat(documentByPriceWithQuery.get("title"), equalTo("Redis Essentials"));
    assertThat(documentByPriceWithQuery.get("price"), equalTo("19.99"));
    assertThat(documentByPriceWithQuery.get("author"), nullValue());
  }

  @Test
  public void testFtSearchJson() {
    String indexName = "testIndex";

    IndexDefinition indexDefinition = new IndexDefinition(IndexDefinition.Type.JSON)
        .setPrefixes("books:");

    IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(indexDefinition);

    Schema schema = new Schema()
        .addField(new Schema.Field("$.title", Schema.FieldType.TEXT))
        .addField(new Schema.Field("$.price", Schema.FieldType.NUMERIC));

    String create = exec(commandObjects.ftCreate(indexName, indexOptions, schema));
    assertThat(create, equalTo("OK"));

    Map<String, Object> hash = new HashMap<>();
    hash.put("title", "Redis in Action");
    hash.put("price", 17.99);
    hash.put("author", "John Doe");

    String jsonSet = exec(commandObjects.jsonSet("books:1000", Path2.ROOT_PATH, new JSONObject(hash)));
    assertThat(jsonSet, equalTo("OK"));

    Map<String, Object> hash2 = new HashMap<>();
    hash2.put("title", "Redis Essentials");
    hash2.put("price", 19.99);
    hash2.put("author", "Jane Doe");

    String jsonSet2 = exec(commandObjects.jsonSet("books:1200", Path2.ROOT_PATH, new JSONObject(hash2)));
    assertThat(jsonSet2, equalTo("OK"));

    SearchResult searchResult = exec(commandObjects.ftSearch(indexName, "Action"));

    assertThat(searchResult.getTotalResults(), equalTo(1L));
    assertThat(searchResult.getDocuments(), hasSize(1));

    Document document = searchResult.getDocuments().get(0);
    assertThat(document.getId(), equalTo("books:1000"));
    assertThat(document.get("$"), equalTo("{\"title\":\"Redis in Action\",\"price\":17.99,\"author\":\"John Doe\"}"));
  }

  @Test
  public void testFtExplain() {
    String indexName = "booksIndex";

    IndexDefinition indexDefinition = new IndexDefinition(IndexDefinition.Type.HASH).setPrefixes("books:");

    IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(indexDefinition);

    Schema schema = new Schema()
        .addField(new Schema.Field("title", Schema.FieldType.TEXT))
        .addField(new Schema.Field("price", Schema.FieldType.NUMERIC));

    String createResult = exec(commandObjects.ftCreate(indexName, indexOptions, schema));
    assertThat(createResult, equalTo("OK"));

    // Add a book to the index
    String bookId = "books:123";

    Map<String, Object> bookFields = new HashMap<>();
    bookFields.put("title", "Redis for Dummies");
    bookFields.put("price", 29.99);

    Long hsetResult = exec(commandObjects.hsetObject(bookId, bookFields));
    assertThat(hsetResult, equalTo(2L));

    Query query = new Query("Redis").returnFields("title", "price");

    String explanation = exec(commandObjects.ftExplain(indexName, query));
    assertThat(explanation, not(emptyOrNullString()));

    List<String> explanationCli = exec(commandObjects.ftExplainCLI(indexName, query));
    assertThat(explanationCli, not(empty()));
  }

  @Test
  public void testFtAggregate() {
    String indexName = "booksIndex";

    IndexDefinition indexDefinition = new IndexDefinition(IndexDefinition.Type.HASH).setPrefixes("books:");

    IndexOptions indexOptions = IndexOptions.defaultOptions().setDefinition(indexDefinition);

    Schema schema = new Schema()
        .addField(new Schema.Field("title", Schema.FieldType.TEXT))
        .addField(new Schema.Field("price", Schema.FieldType.NUMERIC))
        .addField(new Schema.Field("genre", Schema.FieldType.TAG));

    String createResult = exec(commandObjects.ftCreate(indexName, indexOptions, schema));
    assertThat(createResult, equalTo("OK"));

    // Add books to the index
    Map<String, Object> book1Fields = new HashMap<>();
    book1Fields.put("title", "Redis for Dummies");
    book1Fields.put("price", 20.99);
    book1Fields.put("genre", "Technology");

    String book1Id = "books:101";

    exec(commandObjects.hsetObject(book1Id, book1Fields));

    Map<String, Object> book2Fields = new HashMap<>();
    book2Fields.put("title", "Advanced Redis");
    book2Fields.put("price", 25.99);
    book2Fields.put("genre", "Technology");

    String book2Id = "books:102";

    exec(commandObjects.hsetObject(book2Id, book2Fields));

    // Aggregation: average price of books in the 'Technology' genre
    AggregationBuilder aggr = new AggregationBuilder()
        .groupBy("@genre", Reducers.avg("@price").as("avgPrice"))
        .filter("@genre=='Technology'");

    AggregationResult aggregationResult = exec(commandObjects.ftAggregate(indexName, aggr));

    assertThat(aggregationResult, notNullValue());
    assertThat(aggregationResult.getResults(), hasSize(1));

    Map<String, Object> result = aggregationResult.getResults().get(0);
    assertThat(result, hasEntry("genre", "Technology"));
    assertThat(result, hasEntry("avgPrice", "23.49"));
  }
}
