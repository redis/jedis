package redis.clients.jedis.resps;

import java.util.Collections;
import java.util.List;

/**
 * Result for STRALGO LCS command.
 */
public class LCSMatchResult {
    private String matchString;

    private List<MatchedPosition> matches;

    private long len;

    public LCSMatchResult(String matchString) {
        this.matchString = matchString;
    }

    public LCSMatchResult(long len) {
        this.len = len;
    }

    public LCSMatchResult(List<MatchedPosition> matches, long len) {
        this.matches = matches;
        this.len = len;
    }

    /**
     * Creates new {@link LCSMatchResult}.
     *
     * @param matchString
     * @param matches
     * @param len
     */
    public LCSMatchResult(String matchString, List<MatchedPosition> matches, long len) {
        this.matchString = matchString;
        this.matches = Collections.unmodifiableList(matches);
        this.len = len;
    }

    public String getMatchString() {
        return matchString;
    }

    public List<MatchedPosition> getMatches() {
        return matches;
    }

    public long getLen() {
        return len;
    }

    /**
     * Match position in each string.
     */
    public static class MatchedPosition {

        private final Position a;

        private final Position b;

        private final long matchLen;

        public MatchedPosition(Position a, Position b, long matchLen) {
            this.a = a;
            this.b = b;
            this.matchLen = matchLen;
        }

        public Position getA() {
            return a;
        }

        public Position getB() {
            return b;
        }

        public long getMatchLen() {
            return matchLen;
        }
    }

    /**
     * Position range.
     */
    public static class Position {

        private final long start;

        private final long end;

        public Position(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }
    }
}
