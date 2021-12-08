package redis.clients.jedis.modules.search;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.search.Schema;

public class SchemaTest {

  private final static String TITLE = "title";
  private final static String GENRE = "genre";
  private final static String VOTES = "votes";
  private final static String RATING = "rating";
  private final static String RELEASE_YEAR = "release_year";
  private final static String PLOT = "plot";

  @Test
  public void printSchemaTest() throws Exception {
    Schema sc = new Schema()
        .addTextField(TITLE, 5.0)
        .addSortableTextField(PLOT, 1.0)
        .addSortableTagField(GENRE, ",")
        .addSortableNumericField(RELEASE_YEAR)
        .addSortableNumericField(RATING)
        .addSortableNumericField(VOTES);

    String schemaPrint = sc.toString();
    Assert.assertThat(schemaPrint, CoreMatchers.startsWith("Schema{fields=[TextField{name='title'"));
    Assert.assertThat(schemaPrint, CoreMatchers.containsString("{name='release_year', type=NUMERIC, sortable=true, noindex=false}"));
  }
}
