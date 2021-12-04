package redis.clients.jedis.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

/**
 * IndexOptions encapsulates flags for index creation and should be given to the client on index
 * creation
 *
 * @since 2.0
 */
public class IndexOptions implements IParams {

  /**
   * Set this to tell the index not to save term offset vectors. This reduces memory consumption but
   * does not allow performing exact matches, and reduces overall relevance of multi-term queries
   */
  public static final int USE_TERM_OFFSETS = 0x01;

  /**
   * If set (default), we keep flags per index record telling us what fields the term appeared on,
   * and allowing us to filter results by field
   */
  public static final int KEEP_FIELD_FLAGS = 0x02;

  /**
   * With each document:term record, store how often the term appears within the document. This can
   * be used for sorting documents by their relevance to the given term.
   */
  public static final int KEEP_TERM_FREQUENCIES = 0x08;

  public static final int DEFAULT_FLAGS = USE_TERM_OFFSETS | KEEP_FIELD_FLAGS | KEEP_TERM_FREQUENCIES;

  private final int flags;
  private List<String> stopwords;
  private long expire = 0L;
  private IndexDefinition definition;

  /**
   * Default constructor
   *
   * @param flags flag mask
   */
  public IndexOptions(int flags) {
    this.flags = flags;
  }

  /**
   * The default indexing options - use term offsets and keep fields flags
   */
  public static IndexOptions defaultOptions() {
    return new IndexOptions(DEFAULT_FLAGS);
  }

  /**
   * Set a custom stopword list
   *
   * @param stopwords the list of stopwords
   * @return the options object itself, for builder-style construction
   */
  public IndexOptions setStopwords(String... stopwords) {
    this.stopwords = Arrays.asList(stopwords);
    return this;
  }

  /**
   * Set the index to contain no stopwords, overriding the default list
   *
   * @return the options object itself, for builder-style constructions
   */
  public IndexOptions setNoStopwords() {
    stopwords = new ArrayList<>(0);
    return this;
  }

  /**
   * Temporary
   *
   * @param expire
   * @return IndexOptions
   */
  public IndexOptions setTemporary(long expire) {
    this.expire = expire;
    return this;
  }

  public IndexDefinition getDefinition() {
    return definition;
  }

  public IndexOptions setDefinition(IndexDefinition definition) {
    this.definition = definition;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (definition != null) {
      definition.addParams(args);
    }

    if ((flags & USE_TERM_OFFSETS) == 0) {
      args.add(SearchKeyword.NOOFFSETS.name());
    }
    if ((flags & KEEP_FIELD_FLAGS) == 0) {
      args.add(SearchKeyword.NOFIELDS.name());
    }
    if ((flags & KEEP_TERM_FREQUENCIES) == 0) {
      args.add(SearchKeyword.NOFREQS.name());
    }
    if (expire > 0) {
      args.add(SearchKeyword.TEMPORARY.name());
      args.add(Long.toString(this.expire));
    }

    if (stopwords != null) {
      args.add(SearchKeyword.STOPWORDS.name());
      args.add(Integer.toString(stopwords.size()));
      if (!stopwords.isEmpty()) {
        args.addObjects(stopwords);
      }
    }
  }
}
