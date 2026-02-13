package redis.clients.jedis.search;

import redis.clients.jedis.annots.Experimental;

import java.util.Collections;
import java.util.List;

/**
 * Factory class for creating {@link Scorer} instances for text search.
 * @see Scorer
 */
@Experimental
public class Scorers {

  // Predefined Scorer instances
  private static final Scorer TFIDF = scorer("TFIDF");
  private static final Scorer TFIDF_DOCNORM = scorer("TFIDF.DOCNORM");
  private static final Scorer BM25STD = scorer("BM25STD");
  private static final Scorer BM25STD_NORM = scorer("BM25STD.NORM");
  private static final Scorer DISMAX = scorer("DISMAX");
  private static final Scorer DOCSCORE = scorer("DOCSCORE");
  private static final Scorer HAMMING = scorer("HAMMING");

  private Scorers() {
  }

  private static Scorer scorer(String name) {
    return new Scorer(name) {
    };
  }

  public static Scorer tfidf() {
    return TFIDF;
  }

  public static Scorer tfidfDocnorm() {
    return TFIDF_DOCNORM;
  }

  public static Scorer bm25std() {
    return BM25STD;
  }

  public static Scorer bm25stdNorm() {
    return BM25STD_NORM;
  }

  public static Scorer dismax() {
    return DISMAX;
  }

  public static Scorer docscore() {
    return DOCSCORE;
  }

  public static Scorer hamming() {
    return HAMMING;
  }
}
