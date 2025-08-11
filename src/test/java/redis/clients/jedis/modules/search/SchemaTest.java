package redis.clients.jedis.modules.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
  public void printSvsVamanaSchemaTest() throws Exception {
    Map<String, Object> vamanaAttributes = new HashMap<>();
    vamanaAttributes.put("TYPE", "FLOAT32");
    vamanaAttributes.put("DIM", 128);
    vamanaAttributes.put("DISTANCE_METRIC", "COSINE");
    vamanaAttributes.put("COMPRESSION", "LVQ8");

    Schema sc = new Schema()
        .addTextField(TITLE, 5.0)
        .addVectorField("embedding", Schema.VectorField.VectorAlgo.SVS_VAMANA, vamanaAttributes);

    String schemaPrint = sc.toString();
    assertThat(schemaPrint, Matchers.containsString("VectorField{name='embedding', type=VECTOR, algorithm=SVS_VAMANA"));
    assertThat(schemaPrint, Matchers.containsString("TYPE=FLOAT32"));
    assertThat(schemaPrint, Matchers.containsString("COMPRESSION=LVQ8"));
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

  @Test
  public void addSvsVamanaVectorFieldBasicTest() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("TYPE", "FLOAT32");
    attributes.put("DIM", 256);
    attributes.put("DISTANCE_METRIC", "L2");

    Schema schema = new Schema()
        .addTextField(TITLE, 1.0)
        .addSvsVamanaVectorField("embedding", attributes);

    // Verify the schema contains the correct number of fields
    assertThat(schema.fields.size(), Matchers.equalTo(2));

    // Verify the vector field is correctly configured
    Schema.VectorField vectorField = (Schema.VectorField) schema.fields.get(1);
    assertThat(vectorField.toString(), Matchers.containsString("VectorField{name='embedding', type=VECTOR, algorithm=SVS_VAMANA"));

    // Verify the schema string representation
    String schemaString = schema.toString();
    assertThat(schemaString, Matchers.containsString("algorithm=SVS_VAMANA"));
    assertThat(schemaString, Matchers.containsString("TYPE=FLOAT32"));
    assertThat(schemaString, Matchers.containsString("DIM=256"));
    assertThat(schemaString, Matchers.containsString("DISTANCE_METRIC=L2"));
  }
}
