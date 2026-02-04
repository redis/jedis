package redis.clients.jedis.search;

import redis.clients.jedis.annots.Experimental;

import java.util.Collections;
import java.util.List;

/**
 * Factory class for creating {@link Scorer} instances for text search in FT.HYBRID command.
 */
@Experimental
public class Scorers {

  private static Scorer simpleScorer(String name) {
    return new Scorer(name) {
      @Override
      protected List<Object> getOwnArgs() {
        return Collections.emptyList();
      }
    };
  }

  public static Scorer tfidf() {
    return simpleScorer("TFIDF");
  }

  public static Scorer tfidfDocnorm() {
    return simpleScorer("TFIDF.DOCNORM");
  }

  public static Scorer bm25std() {
    return simpleScorer("BM25STD");
  }

  public static Scorer bm25stdNorm() {
    return simpleScorer("BM25STD.NORM");
  }

  public static Scorer dismax() {
    return simpleScorer("DISMAX");
  }

  public static Scorer docscore() {
    return simpleScorer("DOCSCORE");
  }

  public static Scorer hamming() {
    return simpleScorer("HAMMING");
  }
}

