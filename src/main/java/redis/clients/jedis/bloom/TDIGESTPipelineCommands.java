package redis.clients.jedis.bloom;

import redis.clients.jedis.Response;

import java.util.Map;

public interface TDIGESTPipelineCommands {

    /**
     * {@code TDIGEST.CREATE {key} {compression}}
     *
     * @param key The name of the sketch
     * @param compresstion The compression parameter
     * @return OK
     */
    Response<String> tdigestCreate(String key, long compresstion);

    /**
     * {@code TDIGEST.INFO {key}}
     *
     * @param key The name of the sketch
     * @return Array reply with information about the sketch
     */
    Response<Map<String, Object>> tdigestInfo(String key);

    /**
     * {@code TDIGEST.RESET {key}}
     *
     * @param key The name of the sketch
     * @return OK
     */
    Response<String> tdigestReset(String key);

    /**
     * {@code TDIGEST.ADD {key} [{value} {weight}]}
     *
     * @param key The name of the sketch
     * @param valueWeight Map of 'The value of the observation' -> 'The weight of this observation'
     * @return OK
     */
    Response<String> tdigestAdd(String key, Map<Double, Double> valueWeight);

    /**
     * {@code TDIGEST.MAX {key}}
     *
     * @param key The name of the sketch
     * @return The maximum observation value from the sketch. Return DBL_MIN if the sketch is empty
     */
    Response<String> tdigestMax(String key);

    /**
     * {@code TDIGEST.MIN {key}}
     *
     * @param key The name of the sketch
     * @return The minimum observation value from the sketch. Return DBL_MAX if the sketch is empty
     */
    Response<String> tdigestMin(String key);

    /**
     * {@code TDIGEST.MERGE {to-key} {from-key}}
     *
     * @param to Sketch to copy observation values to
     * @param from Sketch to copy observation values from
     * @return OK on success, error otherwise
     */
    Response<String> tdigestMerge(String to, String from);

    /**
     * {@code TDIGEST.CDF {key} {value}}
     *
     * @param key The name of the sketch
     * @param value upper limit of observation value, for which the fraction of all observations added which are <= value
     * @return Estimation of the fraction of all observations added which are <= value
     */
    Response<String> tdigestCdf(String key, double value);

    /**
     * {@code TDIGEST.QUANTILE {key} {quantile ...}}
     *
     * @param key The name of the sketch
     * @param quantile The desired fraction
     * @return Array of results populated with quantile_1, cutoff_1, quantile_2, cutoff_2, ..., quantile_N, cutoff_N.
     */
    Response<Map<String, String>> tdigestQuantile(String key, double... quantile);

    /**
     * {@code TDIGEST.QUANTILE {key} {low_cut_quantile} {high_cut_quantile}}
     *
     * @param key The name of the sketch
     * @param low Exclude observation values lower than this quantile
     * @param high Exclude observation values higher than this quantile
     * @return Estimation of the mean value. Will return DBL_MAX if the sketch is empty.
     */
    Response<String> tdigestTrimmedMean(String key, double low, double high);
}
