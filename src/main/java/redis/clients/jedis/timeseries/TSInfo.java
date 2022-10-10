package redis.clients.jedis.timeseries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.util.DoublePrecision;
import redis.clients.jedis.util.SafeEncoder;

public class TSInfo {

  private static final String DUPLICATE_POLICY_PROPERTY = "duplicatePolicy";
  private static final String LABELS_PROPERTY = "labels";
  private static final String RULES_PROPERTY = "rules";
  private static final String CHUNKS_PROPERTY = "Chunks";
  private static final String CHUNKS_BYTES_PER_SAMPLE_PROPERTY = "bytesPerSample";

  private final Map<String, Object> properties;
  private final Map<String, String> labels;
  private final Map<String, Rule> rules;
  private final List<Map<String, Object>> chunks;

  private TSInfo(Map<String, Object> properties, Map<String, String> labels, Map<String, Rule> rules, List<Map<String, Object>> chunks) {
    this.properties = properties;
    this.labels = labels;
    this.rules = rules;
    this.chunks = chunks;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Object getProperty(String property) {
    return properties.get(property);
  }

  public Long getIntegerProperty(String property) {
    return (Long) properties.get(property);
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public String getLabel(String label) {
    return labels.get(label);
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public Rule getRule(String rule) {
    return rules.get(rule);
  }

  public List<Map<String, Object>> getChunks() {
    return chunks;
  }

  public static Builder<TSInfo> TIMESERIES_INFO = new Builder<TSInfo>() {
    @Override
    public TSInfo build(Object data) {
      List<Object> list = (List<Object>) data;
      Map<String, Object> properties = new HashMap<>();
      Map<String, String> labels = null;
      Map<String, Rule> rules = null;
      List<Map<String, Object>> chunks = null;

      for (int i = 0; i < list.size(); i += 2) {
        String prop = SafeEncoder.encode((byte[]) list.get(i));
        Object value = list.get(i + 1);
        if (value instanceof List) {
          switch (prop) {
            case LABELS_PROPERTY:
              labels = BuilderFactory.STRING_MAP_FROM_PAIRS.build(value);
              value = labels;
              break;
            case RULES_PROPERTY:
              List<Object> rulesDataList = (List<Object>) value;
              List<List<Object>> rulesValueList = new ArrayList<>(rulesDataList.size());
              rules = new HashMap<>(rulesDataList.size());
              for (Object ruleData : rulesDataList) {
                List<Object> encodedRule = (List<Object>) SafeEncoder.encodeObject(ruleData);
                rulesValueList.add(encodedRule);
                rules.put((String) encodedRule.get(0), new Rule((String) encodedRule.get(0), (Long) encodedRule.get(1),
                    AggregationType.safeValueOf((String) encodedRule.get(2)), (Long) encodedRule.get(3)));
              }
              value = rulesValueList;
              break;
            case CHUNKS_PROPERTY:
              List<Object> chunksDataList = (List<Object>) value;
              List<Map<String, Object>> chunksValueList = new ArrayList<>(chunksDataList.size());
              chunks = new ArrayList<>(chunksDataList.size());
              for (Object chunkData : chunksDataList) {
                Map<String, Object> chunk = BuilderFactory.ENCODED_OBJECT_MAP.build(chunkData);
                chunksValueList.add(new HashMap<>(chunk));
                if (chunk.containsKey(CHUNKS_BYTES_PER_SAMPLE_PROPERTY)) {
                  chunk.put(CHUNKS_BYTES_PER_SAMPLE_PROPERTY,
                      DoublePrecision.parseFloatingPointNumber((String) chunk.get(CHUNKS_BYTES_PER_SAMPLE_PROPERTY)));
                }
                chunks.add(chunk);
              }
              value = chunksValueList;
              break;
            default:
              value = SafeEncoder.encodeObject(value);
              break;
          }
        } else if (value instanceof byte[]) {
          value = SafeEncoder.encode((byte[]) value);
          if (DUPLICATE_POLICY_PROPERTY.equals(prop)) {
            try {
              value = DuplicatePolicy.valueOf(((String) value).toUpperCase());
            } catch (Exception e) { }
          }
        }
        properties.put(prop, value);
      }

      return new TSInfo(properties, labels, rules, chunks);
    }
  };

  public static class Rule {

    private final String compactionKey;
    private final long bucketDuration;
    private final AggregationType aggregator;
    private final long alignmentTimestamp;

    private Rule(String compaction, long bucket, AggregationType aggregation, long alignment) {
      this.compactionKey = compaction;
      this.bucketDuration = bucket;
      this.aggregator = aggregation;
      this.alignmentTimestamp = alignment;
    }

    public String getCompactionKey() {
      return compactionKey;
    }

    public long getBucketDuration() {
      return bucketDuration;
    }

    public AggregationType getAggregator() {
      return aggregator;
    }

    public long getAlignmentTimestamp() {
      return alignmentTimestamp;
    }
  }
}
