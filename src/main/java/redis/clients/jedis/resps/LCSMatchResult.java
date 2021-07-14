/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package redis.clients.jedis.resps;

import java.util.Collections;
import java.util.List;

/**
 * Result for STRALGO LCS command.
 *
 * @author dengliming
 * @author Mark Paluch
 * @author yangbodong
 * @since 6.0
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
