package redis.clients.jedis.search;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import java.util.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.util.LazyRawable;

/**
 * Query represents query parameters and filters to load results from the engine
 */
public class FTSearchParams implements IParams {

  private boolean noContent = false;
  private boolean verbatim = false;
  private boolean noStopwords = false;
  private boolean withScores = false;
  private final List<IParams> filters = new LinkedList<>();
  private Collection<String> inKeys;
  private Collection<String> inFields;
  private Collection<FieldName> returnFieldsNames;
  private boolean summarize;
  private SummarizeParams summarizeParams;
  private boolean highlight;
  private HighlightParams highlightParams;
  private Integer slop;
  private Long timeout;
  private boolean inOrder;
  private String language;
  private String expander;
  private String scorer;
  // private boolean explainScore; // TODO
  private String sortBy;
  private SortingOrder sortOrder;
  private int[] limit;
  private Map<String, Object> params;
  private Integer dialect;

  /// non command parameters
  private Map<String, Boolean> returnFieldDecodeMap = null;

  public FTSearchParams() {
  }

  public static FTSearchParams searchParams() {
    return new FTSearchParams();
  }

  @Override
  public void addParams(CommandArguments args) {

    if (noContent) {
      args.add(NOCONTENT);
    }
    if (verbatim) {
      args.add(VERBATIM);
    }
    if (noStopwords) {
      args.add(NOSTOPWORDS);
    }
    if (withScores) {
      args.add(WITHSCORES);
    }

    if (!filters.isEmpty()) {
      filters.forEach(filter -> filter.addParams(args));
    }

    if (inKeys != null && !inKeys.isEmpty()) {
      args.add(INKEYS).add(inKeys.size()).addObjects(inKeys);
    }

    if (inFields != null && !inFields.isEmpty()) {
      args.add(INFIELDS).add(inFields.size()).addObjects(inFields);
    }

    if (returnFieldsNames != null && !returnFieldsNames.isEmpty()) {
      args.add(RETURN);
      LazyRawable returnCountObject = new LazyRawable();
      args.add(returnCountObject); // holding a place for setting the total count later.
      int returnCount = 0;
      for (FieldName fn : returnFieldsNames) {
        returnCount += fn.addCommandArguments(args);
      }
      returnCountObject.setRaw(Protocol.toByteArray(returnCount));
    }

    if (summarizeParams != null) {
      args.addParams(summarizeParams);
    } else if (summarize) {
      args.add(SUMMARIZE);
    }

    if (highlightParams != null) {
      args.addParams(highlightParams);
    } else if (highlight) {
      args.add(HIGHLIGHT);
    }

    if (slop != null) {
      args.add(SLOP).add(slop);
    }

    if (timeout != null) {
      args.add(TIMEOUT).add(timeout);
    }

    if (inOrder) {
      args.add(INORDER);
    }

    if (language != null) {
      args.add(LANGUAGE).add(language);
    }

    if (expander != null) {
      args.add(EXPANDER).add(expander);
    }

    if (scorer != null) {
      args.add(SCORER).add(scorer);
    }
//
//    if (explainScore) {
//      args.add(EXPLAINSCORE);
//    }

    if (sortBy != null) {
      args.add(SORTBY).add(sortBy);
      if (sortOrder != null) {
        args.add(sortOrder);
      }
    }

    if (limit != null) {
      args.add(LIMIT).add(limit[0]).add(limit[1]);
    }

    if (params != null && !params.isEmpty()) {
      args.add(PARAMS).add(params.size() << 1);
      params.entrySet().forEach(entry -> args.add(entry.getKey()).add(entry.getValue()));
    }

    if (dialect != null) {
      args.add(DIALECT).add(dialect);
    }
  }

  /**
   * Set the query not to return the contents of documents, and rather just return the ids
   *
   * @return the query itself
   */
  public FTSearchParams noContent() {
    this.noContent = true;
    return this;
  }

  /**
   * Set the query to verbatim mode, disabling stemming and query expansion
   *
   * @return the query object
   */
  public FTSearchParams verbatim() {
    this.verbatim = true;
    return this;
  }

  /**
   * Set the query not to filter for stopwords. In general this should not be used
   *
   * @return the query object
   */
  public FTSearchParams noStopwords() {
    this.noStopwords = true;
    return this;
  }

  /**
   * Set the query to return a factored score for each results. This is useful to merge results from
   * multiple queries.
   *
   * @return the query object itself
   */
  public FTSearchParams withScores() {
    this.withScores = true;
    return this;
  }

  public FTSearchParams filter(String field, double min, double max) {
    return filter(new NumericFilter(field, min, max));
  }

  public FTSearchParams filter(String field, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
    return filter(new NumericFilter(field, min, exclusiveMin, max, exclusiveMax));
  }

  public FTSearchParams filter(NumericFilter numericFilter) {
    filters.add(numericFilter);
    return this;
  }

  public FTSearchParams geoFilter(String field, double lon, double lat, double radius, GeoUnit unit) {
    return geoFilter(new GeoFilter(field, lon, lat, radius, unit));
  }

  public FTSearchParams geoFilter(GeoFilter geoFilter) {
    filters.add(geoFilter);
    return this;
  }

  /**
   * Limit the query to results that are limited to a specific set of keys
   *
   * @param keys a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public FTSearchParams inKeys(String... keys) {
    return inKeys(Arrays.asList(keys));
  }

  public FTSearchParams inKeys(Collection<String> keys) {
    this.inKeys = keys;
    return this;
  }

  /**
   * Limit the query to results that are limited to a specific set of fields
   *
   * @param fields a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public FTSearchParams inFields(String... fields) {
    return inFields(Arrays.asList(fields));
  }

  public FTSearchParams inFields(Collection<String> fields) {
    if (this.inFields == null) {
      this.inFields = new ArrayList<>(fields);
    } else {
      this.inFields.addAll(fields);
    }
    return this;
  }

  /**
   * Result's projection - the fields to return by the query
   *
   * @param fields a list of TEXT fields in the schemas
   * @return the query object itself
   */
  public FTSearchParams returnFields(String... fields) {
    if (returnFieldsNames == null) {
      returnFieldsNames = new ArrayList<>();
    }
    Arrays.stream(fields).forEach(f -> returnFieldsNames.add(FieldName.of(f)));
    return this;
  }

  public FTSearchParams returnField(FieldName field) {
    return returnFields(Collections.singleton(field));
  }

  public FTSearchParams returnFields(FieldName... fields) {
    return returnFields(Arrays.asList(fields));
  }

  public FTSearchParams returnFields(Collection<FieldName> fields) {
    if (returnFieldsNames == null) {
      returnFieldsNames = new ArrayList<>();
    }
    returnFieldsNames.addAll(fields);
    return this;
  }

  public FTSearchParams returnField(String field, boolean decode) {
    returnFields(field);
    addReturnFieldDecode(field, decode);
    return this;
  }

  public FTSearchParams returnField(FieldName field, boolean decode) {
    returnFields(field);
    addReturnFieldDecode(field.getAttribute() != null ? field.getAttribute() : field.getName(), decode);
    return this;
  }

  private void addReturnFieldDecode(String returnName, boolean decode) {
    if (returnFieldDecodeMap == null) {
      returnFieldDecodeMap = new HashMap<>();
    }
    returnFieldDecodeMap.put(returnName, decode);
  }

  public FTSearchParams summarize() {
    this.summarize = true;
    return this;
  }

  public FTSearchParams summarize(SummarizeParams summarizeParams) {
    this.summarizeParams = summarizeParams;
    return this;
  }

  public FTSearchParams highlight() {
    this.highlight = true;
    return this;
  }

  public FTSearchParams highlight(HighlightParams highlightParams) {
    this.highlightParams = highlightParams;
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
  public FTSearchParams scorer(String scorer) {
    this.scorer = scorer;
    return this;
  }
//
//  public FTSearchParams explainScore() {
//    this.explainScore = true;
//    return this;
//  }

  public FTSearchParams slop(int slop) {
    this.slop = slop;
    return this;
  }

  public FTSearchParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  public FTSearchParams inOrder() {
    this.inOrder = true;
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
  public FTSearchParams language(String language) {
    this.language = language;
    return this;
  }

  /**
   * Set the query to be sorted by a Sortable field defined in the schema
   *
   * @param sortBy the sorting field's name
   * @param order the sorting order
   * @return the query object itself
   */
  public FTSearchParams sortBy(String sortBy, SortingOrder order) {
    this.sortBy = sortBy;
    this.sortOrder = order;
    return this;
  }

  /**
   * Limit the results to a certain offset and limit
   *
   * @param offset the first result to show, zero based indexing
   * @param num how many results we want to show
   * @return the query itself, for builder-style syntax
   */
  public FTSearchParams limit(int offset, int num) {
    this.limit = new int[]{offset, num};
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
  public FTSearchParams addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<>();
    }
    params.put(name, value);
    return this;
  }

  public FTSearchParams params(Map<String, Object> paramValues) {
    if (this.params == null) {
      this.params = new HashMap<>(paramValues);
    } else {
      this.params.putAll(params);
    }
    return this;
  }

  /**
   * Set the dialect version to execute the query accordingly
   *
   * @param dialect integer
   * @return the query object itself
   */
  public FTSearchParams dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  /**
   * This method will not replace the dialect if it has been already set.
   * @param dialect dialect
   * @return this
   */
  @Internal
  public FTSearchParams dialectOptional(int dialect) {
    if (dialect != 0 && this.dialect == null) {
      this.dialect = dialect;
    }
    return this;
  }

  @Internal
  public boolean getNoContent() {
    return noContent;
  }

  @Internal
  public boolean getWithScores() {
    return withScores;
  }

  @Internal
  public Map<String, Boolean> getReturnFieldDecodeMap() {
    return returnFieldDecodeMap;
  }

  /**
   * NumericFilter wraps a range filter on a numeric field. It can be inclusive or exclusive
   */
  public static class NumericFilter implements IParams {

    private final String field;
    private final double min;
    private final boolean exclusiveMin;
    private final double max;
    private final boolean exclusiveMax;

    public NumericFilter(String field, double min, double max) {
      this(field, min, false, max, false);
    }

    public NumericFilter(String field, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
      this.field = field;
      this.min = min;
      this.max = max;
      this.exclusiveMax = exclusiveMax;
      this.exclusiveMin = exclusiveMin;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(FILTER).add(field)
          .add(formatNum(min, exclusiveMin))
          .add(formatNum(max, exclusiveMax));
    }

    private Object formatNum(double num, boolean exclude) {
      return exclude ? ("(" + num) : Protocol.toByteArray(num);
    }
  }

  /**
   * GeoFilter encapsulates a radius filter on a geographical indexed fields
   */
  public static class GeoFilter implements IParams {

    private final String field;
    private final double lon;
    private final double lat;
    private final double radius;
    private final GeoUnit unit;

    public GeoFilter(String field, double lon, double lat, double radius, GeoUnit unit) {
      this.field = field;
      this.lon = lon;
      this.lat = lat;
      this.radius = radius;
      this.unit = unit;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(GEOFILTER).add(field)
          .add(lon).add(lat)
          .add(radius).add(unit);
    }
  }

  public static class SummarizeParams implements IParams {

    private Collection<String> fields;
    private Integer fragsNum;
    private Integer fragSize;
    private String separator;

    public SummarizeParams() {
    }

    public SummarizeParams fields(String... fields) {
      return fields(Arrays.asList(fields));
    }

    public SummarizeParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    public SummarizeParams fragsNum(int num) {
      this.fragsNum = num;
      return this;
    }

    public SummarizeParams fragSize(int size) {
      this.fragSize = size;
      return this;
    }

    public SummarizeParams separator(String separator) {
      this.separator = separator;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(SUMMARIZE);

      if (fields != null) {
        args.add(FIELDS).add(fields.size()).addObjects(fields);
      }
      if (fragsNum != null) {
        args.add(FRAGS).add(fragsNum);
      }
      if (fragSize != null) {
        args.add(LEN).add(fragSize);
      }
      if (separator != null) {
        args.add(SEPARATOR).add(separator);
      }
    }
  }

  public static SummarizeParams summarizeParams() {
    return new SummarizeParams();
  }

  public static class HighlightParams implements IParams {

    private Collection<String> fields;
    private String[] tags;

    public HighlightParams() {
    }

    public HighlightParams fields(String fields) {
      return fields(Arrays.asList(fields));
    }

    public HighlightParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    public HighlightParams tags(String open, String close) {
      this.tags = new String[]{open, close};
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(HIGHLIGHT);

      if (fields != null) {
        args.add(FIELDS).add(fields.size()).addObjects(fields);
      }
      if (tags != null) {
        args.add(TAGS).add(tags[0]).add(tags[1]);
      }
    }
  }

  public static HighlightParams highlightParams() {
    return new HighlightParams();
  }
}
