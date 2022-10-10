package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.search.SearchProtocol.SearchKeyword;

/**
 * IndexDefinition encapsulates configuration for index definition creation and should be given to
 * the client on index creation
 */
public class IndexDefinition implements IParams {

  public enum Type {
    HASH,
    JSON
  }

  private final Type type;
  private boolean async = false;
  private String[] prefixes;
  private String filter;
  private String languageField;
  private String language;
  private String scoreFiled;
  private double score = 1.0; // Default score when score isn't defined
  private String payloadField;

  public IndexDefinition() {
    this(null);
  }

  public IndexDefinition(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public boolean isAsync() {
    return async;
  }

  public IndexDefinition setAsync(boolean async) {
    this.async = async;
    return this;
  }

  public String[] getPrefixes() {
    return prefixes;
  }

  public IndexDefinition setPrefixes(String... prefixes) {
    this.prefixes = prefixes;
    return this;
  }

  public String getFilter() {
    return filter;
  }

  public IndexDefinition setFilter(String filter) {
    this.filter = filter;
    return this;
  }

  public String getLanguageField() {
    return languageField;
  }

  public IndexDefinition setLanguageField(String languageField) {
    this.languageField = languageField;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public IndexDefinition setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getScoreFiled() {
    return scoreFiled;
  }

  public IndexDefinition setScoreFiled(String scoreFiled) {
    this.scoreFiled = scoreFiled;
    return this;
  }

  public double getScore() {
    return score;
  }

  public IndexDefinition setScore(double score) {
    this.score = score;
    return this;
  }

  public String getPayloadField() {
    return payloadField;
  }

  /**
   * @deprecated Since RediSearch 2.0.0, PAYLOAD_FIELD option is deprecated.
   */
  @Deprecated
  public IndexDefinition setPayloadField(String payloadField) {
    this.payloadField = payloadField;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (type != null) {
      args.add(SearchKeyword.ON.name());
      args.add(type.name());
    }

    if (async) {
      args.add(SearchKeyword.ASYNC.name());
    }

    if (prefixes != null && prefixes.length > 0) {
      args.add(SearchKeyword.PREFIX.name());
      args.add(Integer.toString(prefixes.length));
      args.addObjects((Object[]) prefixes);
    }

    if (filter != null) {
      args.add(SearchKeyword.FILTER.name());
      args.add(filter);
    }

    if (languageField != null) {
      args.add(SearchKeyword.LANGUAGE_FIELD.name());
      args.add(languageField);
    }

    if (language != null) {
      args.add(SearchKeyword.LANGUAGE.name());
      args.add(language);
    }

    if (scoreFiled != null) {
      args.add(SearchKeyword.SCORE_FIELD.name());
      args.add(scoreFiled);
    }

    if (score != 1.0) {
      args.add(SearchKeyword.SCORE.name());
      args.add(Double.toString(score));
    }

    if (payloadField != null) {
      args.add(SearchKeyword.PAYLOAD_FIELD.name());
      args.add(payloadField);
    }
  }
}
