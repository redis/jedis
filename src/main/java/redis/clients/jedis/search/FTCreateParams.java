package redis.clients.jedis.search;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

public class FTCreateParams implements IParams {

  private IndexDataType dataType;
  private Collection<String> prefix;
  private String filter;
  private String language;
  private String languageField;
  private Double score;
  private String scoreField;
  private boolean maxTextFields;
  private boolean noOffsets;
  private Long temporary;
  private boolean noHL;
  private boolean noFields;
  private boolean noFreqs;
  private Collection<String> stopwords;
  private boolean skipInitialScan;

  public FTCreateParams() {
  }

  public static FTCreateParams createParams() {
    return new FTCreateParams();
  }

  /**
   * Currently supports HASH (default) and JSON. To index JSON, you must have the RedisJSON module
   * installed.
   */
  public FTCreateParams on(IndexDataType dataType) {
    this.dataType = dataType;
    return this;
  }

  /**
   * Tells the index which keys it should index. You can add several prefixes to index.
   */
  public FTCreateParams prefix(String... prefixes) {
    if (this.prefix == null) {
      this.prefix = new ArrayList<>(prefixes.length);
    }
    Arrays.stream(prefixes).forEach(p -> this.prefix.add(p));
    return this;
  }

  /**
   * This method can be chained to add multiple prefixes.
   *
   * @see FTCreateParams#prefix(java.lang.String...)
   */
  public FTCreateParams addPrefix(String prefix) {
    if (this.prefix == null) {
      this.prefix = new ArrayList<>();
    }
    this.prefix.add(prefix);
    return this;
  }

  /**
   * A filter expression with the full RediSearch aggregation expression language.
   */
  public FTCreateParams filter(String filter) {
    this.filter = filter;
    return this;
  }

  /**
   * Indicates the default language for documents in the index.
   */
  public FTCreateParams language(String defaultLanguage) {
    this.language = defaultLanguage;
    return this;
  }

  /**
   * Document attribute set as the document language.
   */
  public FTCreateParams languageField(String languageAttribute) {
    this.languageField = languageAttribute;
    return this;
  }

  /**
   * Default score for documents in the index.
   */
  public FTCreateParams score(double defaultScore) {
    this.score = defaultScore;
    return this;
  }

  /**
   * Document attribute that you use as the document rank based on the user ranking.
   * Ranking must be between 0.0 and 1.0.
   */
  public FTCreateParams scoreField(String scoreField) {
    this.scoreField = scoreField;
    return this;
  }

  /**
   * Forces RediSearch to encode indexes as if there were more than 32 text attributes.
   */
  public FTCreateParams maxTextFields() {
    this.maxTextFields = true;
    return this;
  }

  /**
   * Does not store term offsets for documents. It saves memory, but does not allow exact searches
   * or highlighting.
   */
  public FTCreateParams noOffsets() {
    this.noOffsets = true;
    return this;
  }

  /**
   * Creates a lightweight temporary index that expires after a specified period of inactivity.
   */
  public FTCreateParams temporary(long seconds) {
    this.temporary = seconds;
    return this;
  }

  /**
   * Conserves storage space and memory by disabling highlighting support.
   */
  public FTCreateParams noHL() {
    this.noHL = true;
    return this;
  }

  /**
   * @see FTCreateParams#noHL()
   */
  public FTCreateParams noHighlights() {
    return noHL();
  }

  /**
   * Does not store attribute bits for each term. It saves memory, but it does not allow filtering
   * by specific attributes.
   */
  public FTCreateParams noFields() {
    this.noFields = true;
    return this;
  }

  /**
   * Avoids saving the term frequencies in the index. It saves memory, but does not allow sorting
   * based on the frequencies of a given term within the document.
   */
  public FTCreateParams noFreqs() {
    this.noFreqs = true;
    return this;
  }

  /**
   * Sets the index with a custom stopword list, to be ignored during indexing and search time.
   */
  public FTCreateParams stopwords(String... stopwords) {
    this.stopwords = Arrays.asList(stopwords);
    return this;
  }

  /**
   * The index does not have stopwords, not even the default ones.
   */
  public FTCreateParams noStopwords() {
    this.stopwords = Collections.emptyList();
    return this;
  }

  /**
   * Does not scan and index.
   */
  public FTCreateParams skipInitialScan() {
    this.skipInitialScan = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (dataType != null) {
      args.add(ON).add(dataType);
    }

    if (prefix != null) {
      args.add(PREFIX).add(prefix.size()).addObjects(prefix);
    }

    if (filter != null) {
      args.add(FILTER).add(filter);
    }

    if (language != null) {
      args.add(LANGUAGE).add(language);
    }
    if (languageField != null) {
      args.add(LANGUAGE_FIELD).add(languageField);
    }

    if (score != null) {
      args.add(SCORE).add(score);
    }
    if (scoreField != null) {
      args.add(SCORE_FIELD).add(scoreField);
    }

    if (maxTextFields) {
      args.add(MAXTEXTFIELDS);
    }

    if (noOffsets) {
      args.add(NOOFFSETS);
    }

    if (temporary != null) {
      args.add(TEMPORARY).add(temporary);
    }

    if (noHL) {
      args.add(NOHL);
    }

    if (noFields) {
      args.add(NOFIELDS);
    }

    if (noFreqs) {
      args.add(NOFREQS);
    }

    if (stopwords != null) {
      args.add(STOPWORDS).add(stopwords.size());
      stopwords.forEach(w -> args.add(w));
    }

    if (skipInitialScan) {
      args.add(SKIPINITIALSCAN);
    }
  }
}
