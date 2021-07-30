package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.util.SafeEncoder;

public class StrAlgoLCSParams extends Params {

    private static final String LCS = "lcs";
    private static final String IDX = "idx";
    private static final String LEN = "len";
    private static final String WITHMATCHLEN = "withmatchlen";
    private static final String MINMATCHLEN = "minmatchlen";

    public StrAlgoLCSParams() {
    }

    public static StrAlgoLCSParams StrAlgoLCSParams() {
        return new StrAlgoLCSParams();
    }

    /**
     * When IDX is given the command returns an array with the LCS length
     * and all the ranges in both the strings, start and end offset for
     * each string, where there are matches.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams idx() {
        addParam(IDX);
        return this;
    }

    /**
     * When LEN is given the command returns the length of the longest common substring.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams len() {
        addParam(LEN);
        return this;
    }

    /**
     * When WITHMATCHLEN is given each array representing a match will also have the length of the match.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams withMatchLen() {
        addParam(WITHMATCHLEN);
        return this;
    }

    /**
     * Specify the minimum match length.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams minMatchLen(long minMatchLen) {
        addParam(MINMATCHLEN, minMatchLen);
        return this;
    }

    public byte[][] getByteParams(Keyword keyword, byte[] argA, byte[] argB) {
        ArrayList<byte[]> byteParams = new ArrayList<>();
        byteParams.add(SafeEncoder.encode(LCS));
        byteParams.add(keyword.getRaw());
        byteParams.add(argA);
        byteParams.add(argB);

        if (contains(IDX)) {
            byteParams.add(SafeEncoder.encode(IDX));
        }
        if (contains(LEN)) {
            byteParams.add(SafeEncoder.encode(LEN));
        }
        if (contains(WITHMATCHLEN)) {
            byteParams.add(SafeEncoder.encode(WITHMATCHLEN));
        }

        if (contains(MINMATCHLEN)) {
            byteParams.add(SafeEncoder.encode(MINMATCHLEN));
            byteParams.add(Protocol.toByteArray((long) getParam(MINMATCHLEN)));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
