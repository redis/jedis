package redis.clients.jedis.search;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class SearchProtocol {

  public enum SearchCommand implements ProtocolCommand {

    CREATE("FT.CREATE"),
    ALTER("FT.ALTER"),
    ADD("FT.ADD"),
    INFO("FT.INFO"),
    SEARCH("FT.SEARCH"),
    EXPLAIN("FT.EXPLAIN"),
    DEL("FT.DEL"),
    DROP("FT.DROP"),
    GET("FT.GET"),
    MGET("FT.MGET"),
    AGGREGATE("FT.AGGREGATE"),
    CURSOR("FT.CURSOR"),
    CONFIG("FT.CONFIG"),
    ALIASADD("FT.ALIASADD"),
    ALIASUPDATE("FT.ALIASUPDATE"),
    ALIASDEL("FT.ALIASDEL"),
    SYNUPDATE("FT.SYNUPDATE"),
    SYNDUMP("FT.SYNDUMP");

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

    SCHEMA, VERBATIM, NOCONTENT, NOSTOPWORDS, WITHSCORES, WITHPAYLOADS, LANGUAGE, INFIELDS,
    SORTBY, ASC, DESC, PAYLOAD, LIMIT, HIGHLIGHT, FIELDS, TAGS, SUMMARIZE, FRAGS, LEN, SEPARATOR,
    INKEYS, RETURN, NOSAVE, PARTIAL, REPLACE, FILTER, GEOFILTER, POSITIVE_INFINITY("+inf"), NEGATIVE_INFINITY("-inf"),
    INCR, MAX, FUZZY, DD, DELETE, READ, COUNT, ADD, TEMPORARY, STOPWORDS, NOFREQS, NOFIELDS, NOOFFSETS, IF,
    SET, GET, ON, ASYNC, PREFIX, LANGUAGE_FIELD, SCORE_FIELD, SCORE, PAYLOAD_FIELD, SCORER;

    private final byte[] raw;

    private SearchKeyword(String keyword) {
      raw = SafeEncoder.encode(keyword);
    }

    private SearchKeyword() {
      raw = SafeEncoder.encode(this.name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
