package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class TextField extends SchemaField {

  private boolean sortable;
  private boolean sortableUNF;
  private boolean noStem;
  private boolean noIndex;
  private String phoneticMatcher;
  private Double weight;
  private boolean withSuffixTrie;

  public TextField(String fieldName) {
    super(fieldName);
  }

  public TextField(FieldName fieldName) {
    super(fieldName);
  }

  public static TextField of(String fieldName) {
    return new TextField(fieldName);
  }

  public static TextField of(FieldName fieldName) {
    return new TextField(fieldName);
  }

  @Override
  public TextField as(String attribute) {
    super.as(attribute);
    return this;
  }

  /**
   * Sorts the results by the value of this field.
   */
  public TextField sortable() {
    this.sortable = true;
    return this;
  }

  /**
   * Sorts the results by the value of this field without normalization.
   */
  public TextField sortableUNF() {
    this.sortableUNF = true;
    return this;
  }

  /**
   * @see TextField#sortableUNF()
   */
  public TextField sortableUnNormalizedForm() {
    return sortableUNF();
  }

  /**
   * Disable stemming when indexing.
   */
  public TextField noStem() {
    this.noStem = true;
    return this;
  }

  /**
   * Avoid indexing.
   */
  public TextField noIndex() {
    this.noIndex = true;
    return this;
  }

  /**
   * Perform phonetic matching.
   */
  public TextField phonetic(String matcher) {
    this.phoneticMatcher = matcher;
    return this;
  }

  /**
   * Declares the importance of this attribute when calculating result accuracy. This is a
   * multiplication factor.
   */
  public TextField weight(double weight) {
    this.weight = weight;
    return this;
  }

  /**
   * Keeps a suffix trie with all terms which match the suffix. It is used to optimize
   * <i>contains</i> and <i>suffix</i> queries.
   */
  public TextField withSuffixTrie() {
    this.withSuffixTrie = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(TEXT);

    if (sortableUNF) {
      args.add(SORTABLE).add(UNF);
    } else if (sortable) {
      args.add(SORTABLE);
    }

    if (noStem) {
      args.add(NOSTEM);
    }
    if (noIndex) {
      args.add(NOINDEX);
    }

    if (phoneticMatcher != null) {
      args.add(PHONETIC).add(phoneticMatcher);
    }

    if (weight != null) {
      args.add(WEIGHT).add(weight);
    }

    if (withSuffixTrie) {
      args.add(WITHSUFFIXTRIE);
    }
  }
}
