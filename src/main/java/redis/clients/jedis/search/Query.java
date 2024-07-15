package redis.clients.jedis.search;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;
import redis.clients.jedis.util.LazyRawable;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Query represents query parameters and filters to load results from the engine
 */
public class Query implements IParams {

  /**
   * Filter represents a filtering rules in a query
   */
  public abstract static class Filter implements IParams {

    public final String property;

    public Filter(String property) {
      this.property = property;
    }
  }

  /**
   * NumericFilter wraps a range filter on a numeric field. It can be inclusive or exclusive
   */
  public static class NumericFilter extends Filter {

    private final double min;
    private final boolean exclusiveMin;
    private final double max;
    private final boolean exclusiveMax;

    public NumericFilter(String property, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
      super(property);
      this.min = min;
      this.max = max;
      this.exclusiveMax = exclusiveMax;
      this.exclusiveMin = exclusiveMin;
    }

    public NumericFilter(String property, double min, double max) {
      this(property, min, false, max, false);
    }

    private byte[] formatNum(double num, boolean exclude) {
      return exclude ? SafeEncoder.encode("(" + num) : Protocol.toByteArray(num);
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(SearchKeyword.FILTER.getRaw());
      args.add(SafeEncoder.encode(property));
      args.add(formatNum(min, exclusiveMin));
      args.add(formatNum(max, exclusiveMax));
    }
  }

  /**
   * GeoFilter encapsulates a radius filter on a geographical indexed fields
   */
  public static class GeoFilter extends Filter {

    public static final String KILOMETERS = "km";
    public static final String METERS = "m";
    public static final String FEET = "ft";
    public static final String MILES = "mi";

    private final double lon;
    private final double lat;
    private final double radius;
    private final String unit;

    public GeoFilter(String property, double lon, double lat, double radius, String unit) {
      super(property);
      this.lon = lon;
      this.lat = lat;
      this.radius = radius;
      this.unit = unit;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(SearchKeyword.GEOFILTER.getRaw());
      args.add(SafeEncoder.encode(property));
      args.add(Protocol.toByteArray(lon));
      args.add(Protocol.toByteArray(lat));
      args.add(Protocol.toByteArray(radius));
      args.add(SafeEncoder.encode(unit));
    }
  }

  public static class Paging {

    int offset;
    int num;

    public Paging(int offset, int num) {
      this.offset = offset;
      this.num = num;
    }
  }

  public static class HighlightTags {

    private final String open;
    private final String close;

    public HighlightTags(String open, String close) {
      this.open = open;
      this.close = close;
    }
  }

  /**
   * The query's filter list. We only support AND operation on all those filters
   */
  private final List<Filter> _filters = new LinkedList<>();

  /**
   * The textual part of the query
   */
  private final String _queryString;

  /**
   * The sorting parameters
   */
  private final Paging _paging = new Paging(0, 10);

  private boolean _verbatim = false;
  private boolean _noContent = false;
  private boolean _noStopwords = false;
  private boolean _withScores = false;
  private String _language = null;
  private String[] _fields = null;
  private String[] _keys = null;
  private String[] _returnFields = null;
  private FieldName[] returnFieldNames = null;
  private String[] highlightFields = null;
  private String[] summarizeFields = null;
  private String[] highlightTags = null;
  private String summarizeSeparator = null;
  private int summarizeNumFragments = -1;
  private int summarizeFragmentLen = -1;
  private String _sortBy = null;
  private boolean _sortAsc = true;
  private boolean wantsHighlight = false;
  private boolean wantsSummarize = false;
  private String _scorer = null;
  private Map<String, Object> _params = null;
  private Integer _dialect;
  private int _slop = -1;
  private long _timeout = -1;
  private boolean _inOrder = false;
  private String _expander = null;

  public Query() {
    this("*");
  }

  /**
   * Create a new index
   *
   * @param queryString the textual part of the query
   */
  public Query(String queryString) {
    _queryString = queryString;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(SafeEncoder.encode(_queryString));

    if (_verbatim) {
      args.add(SearchKeyword.VERBATIM.getRaw());
    }
    if (_noContent) {
      args.add(SearchKeyword.NOCONTENT.getRaw());
    }
    if (_noStopwords) {
      args.add(SearchKeyword.NOSTOPWORDS.getRaw());
    }
    if (_withScores) {
      args.add(SearchKeyword.WITHSCORES.getRaw());
    }
    if (_language != null) {
      args.add(SearchKeyword.LANGUAGE.getRaw());
      args.add(SafeEncoder.encode(_language));
    }

    if (_scorer != null) {
      args.add(SearchKeyword.SCORER.getRaw());
      args.add(SafeEncoder.encode(_scorer));
    }

    if (_fields != null && _fields.length > 0) {
      args.add(SearchKeyword.INFIELDS.getRaw());
      args.add(Protocol.toByteArray(_fields.length));
      for (String f : _fields) {
        args.add(SafeEncoder.encode(f));
      }
    }

    if (_sortBy != null) {
      args.add(SearchKeyword.SORTBY.getRaw());
      args.add(SafeEncoder.encode(_sortBy));
      args.add((_sortAsc ? SearchKeyword.ASC : SearchKeyword.DESC).getRaw());
    }

    if (_paging.offset != 0 || _paging.num != 10) {
      args.add(SearchKeyword.LIMIT.getRaw()).add(Protocol.toByteArray(_paging.offset)).add(Protocol.toByteArray(_paging.num));
    }

    if (!_filters.isEmpty()) {
      _filters.forEach(filter -> filter.addParams(args));
    }

    if (wantsHighlight) {
      args.add(SearchKeyword.HIGHLIGHT.getRaw());
      if (highlightFields != null) {
        args.add(SearchKeyword.FIELDS.getRaw());
        args.add(Protocol.toByteArray(highlightFields.length));
        for (String s : highlightFields) {
          args.add(SafeEncoder.encode(s));
        }
      }
      if (highlightTags != null) {
        args.add(SearchKeyword.TAGS.getRaw());
        for (String t : highlightTags) {
          args.add(SafeEncoder.encode(t));
        }
      }
    }
    if (wantsSummarize) {
      args.add(SearchKeyword.SUMMARIZE.getRaw());
      if (summarizeFields != null) {
        args.add(SearchKeyword.FIELDS.getRaw());
        args.add(Protocol.toByteArray(summarizeFields.length));
        for (String s : summarizeFields) {
          args.add(SafeEncoder.encode(s));
        }
      }
      if (summarizeNumFragments != -1) {
        args.add(SearchKeyword.FRAGS.getRaw());
        args.add(Protocol.toByteArray(summarizeNumFragments));
      }
      if (summarizeFragmentLen != -1) {
        args.add(SearchKeyword.LEN.getRaw());
        args.add(Protocol.toByteArray(summarizeFragmentLen));
      }
      if (summarizeSeparator != null) {
        args.add(SearchKeyword.SEPARATOR.getRaw());
        args.add(SafeEncoder.encode(summarizeSeparator));
      }
    }

    if (_keys != null && _keys.length > 0) {
      args.add(SearchKeyword.INKEYS.getRaw());
      args.add(Protocol.toByteArray(_keys.length));
      for (String f : _keys) {
        args.add(SafeEncoder.encode(f));
      }
    }

    if (_returnFields != null && _returnFields.length > 0) {
      args.add(SearchKeyword.RETURN.getRaw());
      args.add(Protocol.toByteArray(_returnFields.length));
      for (String f : _returnFields) {
        args.add(SafeEncoder.encode(f));
      }
    } else if (returnFieldNames != null && returnFieldNames.length > 0) {
      args.add(SearchKeyword.RETURN.getRaw());
//      final int returnCountIndex = args.size();
      LazyRawable returnCountObject = new LazyRawable();
//      args.add(null); // holding a place for setting the total count later.
      args.add(returnCountObject); // holding a place for setting the total count later.
      int returnCount = 0;
      for (FieldName fn : returnFieldNames) {
        returnCount += fn.addCommandArguments(args);
      }
//      args.set(returnCountIndex, Protocol.toByteArray(returnCount));
      returnCountObject.setRaw(Protocol.toByteArray(returnCount));
    }

    if (_params != null && _params.size() > 0) {
      args.add(SearchKeyword.PARAMS.getRaw());
      args.add(_params.size() << 1);
      for (Map.Entry<String, Object> entry : _params.entrySet()) {
        args.add(entry.getKey());
        args.add(entry.getValue());
      }
    }

    if (_dialect != null) {
      args.add(SearchKeyword.DIALECT.getRaw());
      args.add(_dialect);
    }

    if (_slop >= 0) {
      args.add(SearchKeyword.SLOP.getRaw());
      args.add(_slop);
    }

    if (_timeout >= 0) {
      args.add(SearchKeyword.TIMEOUT.getRaw());
      args.add(_timeout);
    }

    if (_inOrder) {
      args.add(SearchKeyword.INORDER.getRaw());
    }

    if (_expander != null) {
      args.add(SearchKeyword.EXPANDER.getRaw());
      args.add(SafeEncoder.encode(_expander));
    }
  }

  /**
   * Limit the results to a certain offset and limit
   *
   * @param offset the first result to show, zero based indexing
   * @param limit how many results we want to show
   * @return the query itself, for builder-style syntax
   */
  public Query limit(Integer offset, Integer limit) {
    _paging.offset = offset;
    _paging.num = limit;
    return this;
  }

  /**
   * Add a filter to the query's filter list
   *
   * @param f either a numeric or geo filter object
   * @return the query itself
   */
  public Query addFilter(Filter f) {
    _filters.add(f);
    return this;
  }

  /**
   * Set the query to verbatim mode, disabling stemming and query expansion
   *
   * @return the query object
   */
  public Query setVerbatim() {
    this._verbatim = true;
    return this;
  }

  public boolean getNoContent() {
    return _noContent;
  }

  /**
   * Set the query not to return the contents of documents, and rather just return the ids
   *
   * @return the query itself
   */
  public Query setNoContent() {
    this._noContent = true;
    return this;
  }

  /**
   * Set the query not to filter for stopwords. In general this should not be used
   *
   * @return the query object
   */
  public Query setNoStopwords() {
    this._noStopwords = true;
    return this;
  }

  public boolean getWithScores() {
    return _withScores;
  }

  /**
   * Set the query to return a factored score for each results. This is useful to merge results from
   * multiple queries.
   *
   * @return the query object itself
   */
  public Query setWithScores() {
    this._withScores = true;
    return this;
  }

  /**
   * Set the query language, for stemming purposes
   * <p>
   * See http://redisearch.io for documentation on languages and stemming
   *
   * @param language a language.
   *
   * @return the query object itself
   */
  public Query setLanguage(String language) {
    this._language = language;
    return this;
  }

  /**
   * Set the query custom scorer
   * <p>
   * See http://redisearch.io for documentation on extending RediSearch
   *
   * @param scorer a custom scorer.
   *
   * @return the query object itself
   */
  public Query setScorer(String scorer) {
    this._scorer = scorer;
    return this;
  }

  /**
   * Limit the query to results that are limited to a specific set of fields
   *
   * @param fields a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public Query limitFields(String... fields) {
    this._fields = fields;
    return this;
  }

  /**
   * Limit the query to results that are limited to a specific set of keys
   *
   * @param keys a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public Query limitKeys(String... keys) {
    this._keys = keys;
    return this;
  }

  /**
   * Result's projection - the fields to return by the query
   *
   * @param fields a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public Query returnFields(String... fields) {
    this._returnFields = fields;
    this.returnFieldNames = null;
    return this;
  }

  /**
   * Result's projection - the fields to return by the query
   *
   * @param fields a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public Query returnFields(FieldName... fields) {
    this.returnFieldNames = fields;
    this._returnFields = null;
    return this;
  }

  public Query highlightFields(HighlightTags tags, String... fields) {
    if (fields == null || fields.length > 0) {
      highlightFields = fields;
    }
    if (tags != null) {
      highlightTags = new String[]{tags.open, tags.close};
    } else {
      highlightTags = null;
    }
    wantsHighlight = true;
    return this;
  }

  public Query highlightFields(String... fields) {
    return highlightFields(null, fields);
  }

  public Query summarizeFields(int contextLen, int fragmentCount, String separator, String... fields) {
    if (fields == null || fields.length > 0) {
      summarizeFields = fields;
    }
    summarizeFragmentLen = contextLen;
    summarizeNumFragments = fragmentCount;
    summarizeSeparator = separator;
    wantsSummarize = true;
    return this;
  }

  public Query summarizeFields(String... fields) {
    return summarizeFields(-1, -1, null, fields);
  }

  /**
   * Set the query to be sorted by a Sortable field defined in the schema
   *
   * @param field the sorting field's name
   * @param ascending if set to true, the sorting order is ascending, else descending
   * @return the query object itself
   */
  public Query setSortBy(String field, boolean ascending) {
    _sortBy = field;
    _sortAsc = ascending;
    return this;
  }

  /**
   * Parameters can be referenced in the query string by a $ , followed by the parameter name,
   * e.g., $user , and each such reference in the search query to a parameter name is substituted
   * by the corresponding parameter value.
   *
   * @param name
   * @param value can be String, long or float
   * @return the query object itself
   */
  public Query addParam(String name, Object value) {
    if (_params == null) {
      _params = new HashMap<>();
    }
    _params.put(name, value);
    return this;
  }

  /**
   * Set the dialect version to execute the query accordingly
   *
   * @param dialect integer
   * @return the query object itself
   */
  public Query dialect(int dialect) {
    _dialect = dialect;
    return this;
  }

  /**
   * This method will not replace the dialect if it has been already set.
   * @param dialect dialect
   * @return this
   */
  public Query dialectOptional(int dialect) {
    if (dialect != 0 && this._dialect == null) {
      this._dialect = dialect;
    }
    return this;
  }

  /**
   * Set the slop to execute the query accordingly
   *
   * @param slop integer
   * @return the query object itself
   */
  public Query slop(int slop) {
    _slop = slop;
    return this;
  }

  /**
   * Set the timeout to execute the query accordingly
   *
   * @param timeout long
   * @return the query object itself
   */
  public Query timeout(long timeout) {
    _timeout = timeout;
    return this;
  }

  /**
   * Set the query terms appear in the same order in the document as in the query, regardless of the offsets between them
   *
   * @return the query object
   */
  public Query setInOrder() {
    this._inOrder = true;
    return this;
  }

  /**
   * Set the query to use a custom query expander instead of the stemmer
   *
   * @param field the expander field's name
   * @return the query object itself
   */
  public Query setExpander(String field) {
    _expander = field;
    return this;
  }
}
