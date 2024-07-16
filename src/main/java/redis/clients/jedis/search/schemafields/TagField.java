package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.util.SafeEncoder;

public class TagField extends SchemaField {

  private boolean indexMissing;
  private boolean indexEmpty;
  private byte[] separator;
  private boolean caseSensitive;
  private boolean withSuffixTrie;
  private boolean sortable;
  private boolean sortableUNF;
  private boolean noIndex;

  public TagField(String fieldName) {
    super(fieldName);
  }

  public TagField(FieldName fieldName) {
    super(fieldName);
  }

  public static TagField of(String fieldName) {
    return new TagField(fieldName);
  }

  public static TagField of(FieldName fieldName) {
    return new TagField(fieldName);
  }

  @Override
  public TagField as(String attribute) {
    super.as(attribute);
    return this;
  }

  public TagField indexMissing() {
    this.indexMissing = true;
    return this;
  }

  public TagField indexEmpty() {
    this.indexEmpty = true;
    return this;
  }

  /**
   * Indicates how the text contained in the attribute is to be split into individual tags.
   * @param separator
   */
  public TagField separator(char separator) {
    if (separator < 128) {
      this.separator = new byte[]{(byte) separator};
    } else {
      this.separator = SafeEncoder.encode(String.valueOf(separator));
    }
    return this;
  }

  /**
   * Keeps the original letter cases of the tags.
   */
  public TagField caseSensitive() {
    this.caseSensitive = true;
    return this;
  }

  /**
   * Keeps a suffix trie with all terms which match the suffix. It is used to optimize
   * <i>contains</i> and <i>suffix</i> queries.
   */
  public TagField withSuffixTrie() {
    this.withSuffixTrie = true;
    return this;
  }

  /**
   * Sorts the results by the value of this field.
   */
  public TagField sortable() {
    this.sortable = true;
    return this;
  }

  /**
   * Sorts the results by the value of this field without normalization.
   */
  public TagField sortableUNF() {
    this.sortableUNF = true;
    return this;
  }

  /**
   * @deprecated Use {@code TagField#sortableUNF()}.
   * @see TagField#sortableUNF()
   */
  @Deprecated
  public TagField sortableUnNormalizedForm() {
    return sortableUNF();
  }

  /**
   * Avoid indexing.
   */
  public TagField noIndex() {
    this.noIndex = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(TAG);

    if (indexMissing) {
      args.add(INDEXMISSING);
    }
    if (indexEmpty) {
      args.add(INDEXEMPTY);
    }

    if (separator != null) {
      args.add(SEPARATOR).add(separator);
    }

    if (caseSensitive) {
      args.add(CASESENSITIVE);
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
