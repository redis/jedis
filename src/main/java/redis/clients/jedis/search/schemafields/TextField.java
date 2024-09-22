package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class TextField extends SchemaField {

  private boolean indexMissing;
  private boolean indexEmpty;
  private Double weight;
  private boolean noStem;
  private String phoneticMatcher;
  private boolean withSuffixTrie;
  private boolean sortable;
  private boolean sortableUNF;
  private boolean noIndex;

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

  public TextField indexMissing() {
    this.indexMissing = true;
    return this;
  }

  public TextField indexEmpty() {
    this.indexEmpty = true;
    return this;
  }

  /**
   * Declares the importance of this attribute when calculating result accuracy. This is a
   * multiplication factor.
   * @param weight
   */
  public TextField weight(double weight) {
    this.weight = weight;
    return this;
  }

  /**
   * Disable stemming when indexing.
   */
  public TextField noStem() {
    this.noStem = true;
    return this;
  }

  /**
   * Perform phonetic matching.
   * @param matcher
   */
  public TextField phonetic(String matcher) {
    this.phoneticMatcher = matcher;
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
   * @deprecated Use {@code TextField#sortableUNF()}.
   * @see TextField#sortableUNF()
   */
  @Deprecated
  public TextField sortableUnNormalizedForm() {
    return sortableUNF();
  }

  /**
   * Avoid indexing.
   */
  public TextField noIndex() {
    this.noIndex = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(TEXT);

    if (indexMissing) {
      args.add(INDEXMISSING);
    }
    if (indexEmpty) {
      args.add(INDEXEMPTY);
    }

    if (weight != null) {
      args.add(WEIGHT).add(weight);
    }

    if (noStem) {
      args.add(NOSTEM);
    }

    if (phoneticMatcher != null) {
      args.add(PHONETIC).add(phoneticMatcher);
    }

    if (withSuffixTrie) {
      args.add(WITHSUFFIXTRIE);
    }

    if (sortableUNF) {
      args.add(SORTABLE).add(UNF);
    } else if (sortable) {
      args.add(SORTABLE);
    }

    if (noIndex) {
      args.add(NOINDEX);
    }
  }
}
