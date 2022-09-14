package redis.clients.jedis.search;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class SearchProtocol {

  public enum SearchCommand implements ProtocolCommand {

    CREATE("FT.CREATE"),
    ALTER("FT.ALTER"),
    INFO("FT.INFO"),
    SEARCH("FT.SEARCH"),
    EXPLAIN("FT.EXPLAIN"),
    EXPLAINCLI("FT.EXPLAINCLI"),
    AGGREGATE("FT.AGGREGATE"),
    CURSOR("FT.CURSOR"),
    CONFIG("FT.CONFIG"),
    ALIASADD("FT.ALIASADD"),
    ALIASUPDATE("FT.ALIASUPDATE"),
    ALIASDEL("FT.ALIASDEL"),
    SYNUPDATE("FT.SYNUPDATE"),
    SYNDUMP("FT.SYNDUMP"),
    SUGADD("FT.SUGADD"),
    SUGGET("FT.SUGGET"),
    SUGDEL("FT.SUGDEL"),
    SUGLEN("FT.SUGLEN"),
    DROPINDEX("FT.DROPINDEX"),
    DICTADD("FT.DICTADD"),
    DICTDEL("FT.DICTDEL"),
    DICTDUMP("FT.DICTDUMP"),
    TAGVALS("FT.TAGVALS");

    private final byte[] raw;

    private SearchCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public enum SearchKeyword implements Rawable {

    SCHEMA, TEXT, TAG, NUMERIC, GEO, VECTOR, VERBATIM, NOCONTENT, NOSTOPWORDS, WITHSCORES,
    WITHPAYLOADS, LANGUAGE, INFIELDS, SORTBY, ASC, DESC, PAYLOAD, LIMIT, HIGHLIGHT, FIELDS, TAGS,
    SUMMARIZE, FRAGS, LEN, SEPARATOR, INKEYS, RETURN, FILTER, GEOFILTER, INCR, MAX, FUZZY, DD, DEL,
    READ, COUNT, ADD, TEMPORARY, STOPWORDS, NOFREQS, NOFIELDS, NOOFFSETS, NOHL, SET, GET, ON,
    @Deprecated ASYNC, PREFIX, LANGUAGE_FIELD, SCORE, SCORE_FIELD, SCORER, PAYLOAD_FIELD, PARAMS,
    DIALECT, SLOP, TIMEOUT, INORDER, EXPANDER, MAXTEXTFIELDS, SKIPINITIALSCAN, SORTABLE, UNF,
    NOSTEM, NOINDEX, PHONETIC, WEIGHT, CASESENSITIVE, WITHSUFFIXTRIE;

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
