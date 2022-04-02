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
//    SUGADD("FT.SUGADD"),
//    SUGGET("FT.SUGGET"),
//    SUGDEL("FT.SUGDEL"),
//    SUGLEN("FT.SUGLEN"),
    DROPINDEX("FT.DROPINDEX");

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

    SCHEMA, VERBATIM, NOCONTENT, NOSTOPWORDS, WITHSCORES, WITHPAYLOADS, LANGUAGE, INFIELDS, SORTBY,
    ASC, DESC, PAYLOAD, LIMIT, HIGHLIGHT, FIELDS, TAGS, SUMMARIZE, FRAGS, LEN, SEPARATOR, INKEYS,
    RETURN, /*NOSAVE, PARTIAL, REPLACE,*/ FILTER, GEOFILTER, INCR, MAX, FUZZY, DD, /*DELETE,*/ DEL,
    READ, COUNT, ADD, TEMPORARY, STOPWORDS, NOFREQS, NOFIELDS, NOOFFSETS, /*IF,*/ SET, GET, ON,
    ASYNC, PREFIX, LANGUAGE_FIELD, SCORE_FIELD, SCORE, PAYLOAD_FIELD, SCORER, PARAMS, DIALECT;

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
