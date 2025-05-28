package redis.clients.jedis.modules.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.Schema;

public class SchemaTest {

  private final static String TITLE = "title";
  private final static String GENRE = "genre";
  private final static String VOTES = "votes";
  private final static String RATING = "rating";
  private final static String RELEASE_YEAR = "release_year";
  private final static String PLOT = "plot";
  private final static String VECTOR = "vector";

  @Test
  public void printSchemaTest() throws Exception {
    Schema sc = new Schema()
        .addTextField(TITLE, 5.0)
        .addSortableTextField(PLOT, 1.0)
        .addSortableTagField(GENRE, ",")
        .addSortableNumericField(RELEASE_YEAR)
        .addSortableNumericField(RATING)
        .addSortableNumericField(VOTES)
        .addVectorField(VECTOR, Schema.VectorField.VectorAlgo.HNSW, Collections.emptyMap());

    String schemaPrint = sc.toString();
    assertThat(schemaPrint, Matchers.startsWith("Schema{fields=[TextField{name='title'"));
    assertThat(schemaPrint, Matchers.containsString("{name='release_year', type=NUMERIC, sortable=true, noindex=false}"));
    assertThat(schemaPrint, Matchers.containsString("VectorField{name='vector', type=VECTOR, algorithm=HNSW"));
  }

  @Test
  public void fieldAttributeNull() {
    assertThrows(IllegalArgumentException.class, () -> FieldName.of("identifier").as(null));
  }

  @Test
  public void fieldAttributeMultiple() {
    assertThrows(IllegalStateException.class, () -> FieldName.of("identifier").as("attribute").as("attribute"));
    assertThrows(IllegalStateException.class, () -> new FieldName("identifier", "attribute").as("attribute"));
  }
}
