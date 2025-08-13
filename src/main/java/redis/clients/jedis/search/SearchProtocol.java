package redis.clients.jedis.search;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class SearchProtocol {

  public enum SearchCommand implements ProtocolCommand {

    CREATE("FT.CREATE", true),
    ALTER("FT.ALTER", true),
    INFO("FT.INFO", false),
    SEARCH("FT.SEARCH", false),
    EXPLAIN("FT.EXPLAIN", false),
    EXPLAINCLI("FT.EXPLAINCLI", false),
    AGGREGATE("FT.AGGREGATE", false),
    CURSOR("FT.CURSOR", false),
    @Deprecated CONFIG("FT.CONFIG", true),
    ALIASADD("FT.ALIASADD", true),
    ALIASUPDATE("FT.ALIASUPDATE", true),
    ALIASDEL("FT.ALIASDEL", true),
    SYNUPDATE("FT.SYNUPDATE", true),
    SYNDUMP("FT.SYNDUMP", false),
    SUGADD("FT.SUGADD", true),
    SUGGET("FT.SUGGET", false),
    SUGDEL("FT.SUGDEL", true),
    SUGLEN("FT.SUGLEN", false),
    DROPINDEX("FT.DROPINDEX", true),
    DICTADD("FT.DICTADD", true),
    DICTDEL("FT.DICTDEL", true),
    DICTDUMP("FT.DICTDUMP", false),
    SPELLCHECK("FT.SPELLCHECK", false),
    TAGVALS("FT.TAGVALS", false),
    PROFILE("FT.PROFILE", false),
    _LIST("FT._LIST", false);

    private final byte[] raw;

    private final boolean isWriteCommand;

    private SearchCommand(String alt, boolean isWriteCommand) {
      raw = SafeEncoder.encode(alt);
      this.isWriteCommand = isWriteCommand;
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }

    @Override
    public boolean isWriteCommand() {
      return isWriteCommand;
    }
  }

  public enum SearchKeyword implements Rawable {

    SCHEMA, TEXT, TAG, NUMERIC, GEO, GEOSHAPE, VECTOR, VERBATIM, NOCONTENT, NOSTOPWORDS, WITHSCORES,
    LANGUAGE, INFIELDS, SORTBY, ASC, DESC, LIMIT, HIGHLIGHT, FIELDS, TAGS, SUMMARIZE, FRAGS, LEN,
    SEPARATOR, INKEYS, RETURN, FILTER, GEOFILTER, ADD, INCR, MAX, FUZZY, READ, DEL, DD, TEMPORARY,
    STOPWORDS, NOFREQS, NOFIELDS, NOOFFSETS, NOHL, ON, SORTABLE, UNF, PREFIX,
    LANGUAGE_FIELD, SCORE, SCORE_FIELD, SCORER, PARAMS, AS, DIALECT, SLOP, TIMEOUT, INORDER,
    EXPANDER, MAXTEXTFIELDS, SKIPINITIALSCAN, WITHSUFFIXTRIE, NOSTEM, NOINDEX, PHONETIC, WEIGHT,
    CASESENSITIVE, LOAD, APPLY, GROUPBY, MAXIDLE, WITHCURSOR, DISTANCE, TERMS, INCLUDE, EXCLUDE,
    SEARCH, AGGREGATE, QUERY, LIMITED, COUNT, REDUCE, INDEXMISSING, INDEXEMPTY, ADDSCORES,
    @Deprecated SET, @Deprecated GET;

    private final byte[] raw;

    private SearchKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
