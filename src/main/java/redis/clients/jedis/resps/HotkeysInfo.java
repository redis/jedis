package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;
import redis.clients.jedis.util.KeyValue;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static redis.clients.jedis.BuilderFactory.*;

/**
 * Response object for the HOTKEYS GET command. Contains statistics about hot keys tracked by CPU
 * time and network bytes.
 */
public class HotkeysInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  // Field names from the response
  public static final String TRACKING_ACTIVE = "tracking-active";
  public static final String SAMPLE_RATIO = "sample-ratio";
  public static final String SELECTED_SLOTS = "selected-slots";
  public static final String SAMPLED_COMMAND_SELECTED_SLOTS_US = "sampled-command-selected-slots-us";
  public static final String ALL_COMMANDS_SELECTED_SLOTS_US = "all-commands-selected-slots-us";
  public static final String ALL_COMMANDS_ALL_SLOTS_US = "all-commands-all-slots-us";
  public static final String NET_BYTES_SAMPLED_COMMANDS_SELECTED_SLOTS = "net-bytes-sampled-commands-selected-slots";
  public static final String NET_BYTES_ALL_COMMANDS_SELECTED_SLOTS = "net-bytes-all-commands-selected-slots";
  public static final String NET_BYTES_ALL_COMMANDS_ALL_SLOTS = "net-bytes-all-commands-all-slots";
  public static final String COLLECTION_START_TIME_UNIX_MS = "collection-start-time-unix-ms";
  public static final String COLLECTION_DURATION_MS = "collection-duration-ms";
  public static final String TOTAL_CPU_TIME_USER_MS = "total-cpu-time-user-ms";
  public static final String TOTAL_CPU_TIME_SYS_MS = "total-cpu-time-sys-ms";
  public static final String TOTAL_NET_BYTES = "total-net-bytes";
  public static final String BY_CPU_TIME_US = "by-cpu-time-us";
  public static final String BY_NET_BYTES = "by-net-bytes";

  private final boolean trackingActive;
  private final long sampleRatio;
  private final List<int[]> selectedSlots; // List of [start, end] slot ranges
  private final Long sampledCommandSelectedSlotsUs;
  private final Long allCommandsSelectedSlotsUs;
  private final long allCommandsAllSlotsUs;
  private final Long netBytesSampledCommandsSelectedSlots;
  private final Long netBytesAllCommandsSelectedSlots;
  private final long netBytesAllCommandsAllSlots;
  private final long collectionStartTimeUnixMs;
  private final long collectionDurationMs;
  private final long totalCpuTimeUserMs;
  private final long totalCpuTimeSysMs;
  private final long totalNetBytes;
  private final Map<String, Long> byCpuTimeUs;
  private final Map<String, Long> byNetBytes;

  public HotkeysInfo(boolean trackingActive, long sampleRatio, List<int[]> selectedSlots,
      Long sampledCommandSelectedSlotsUs, Long allCommandsSelectedSlotsUs,
      long allCommandsAllSlotsUs, Long netBytesSampledCommandsSelectedSlots,
      Long netBytesAllCommandsSelectedSlots, long netBytesAllCommandsAllSlots,
      long collectionStartTimeUnixMs, long collectionDurationMs, long totalCpuTimeUserMs,
      long totalCpuTimeSysMs, long totalNetBytes, Map<String, Long> byCpuTimeUs,
      Map<String, Long> byNetBytes) {
    this.trackingActive = trackingActive;
    this.sampleRatio = sampleRatio;
    this.selectedSlots = selectedSlots;
    this.sampledCommandSelectedSlotsUs = sampledCommandSelectedSlotsUs;
    this.allCommandsSelectedSlotsUs = allCommandsSelectedSlotsUs;
    this.allCommandsAllSlotsUs = allCommandsAllSlotsUs;
    this.netBytesSampledCommandsSelectedSlots = netBytesSampledCommandsSelectedSlots;
    this.netBytesAllCommandsSelectedSlots = netBytesAllCommandsSelectedSlots;
    this.netBytesAllCommandsAllSlots = netBytesAllCommandsAllSlots;
    this.collectionStartTimeUnixMs = collectionStartTimeUnixMs;
    this.collectionDurationMs = collectionDurationMs;
    this.totalCpuTimeUserMs = totalCpuTimeUserMs;
    this.totalCpuTimeSysMs = totalCpuTimeSysMs;
    this.totalNetBytes = totalNetBytes;
    this.byCpuTimeUs = byCpuTimeUs;
    this.byNetBytes = byNetBytes;
  }

  public boolean isTrackingActive() {
    return trackingActive;
  }

  public long getSampleRatio() {
    return sampleRatio;
  }

  /**
   * Returns the selected slot ranges. Each element is an int array of [start, end] representing a
   * slot range (inclusive).
   * @return list of slot ranges, empty if all slots are selected
   */
  public List<int[]> getSelectedSlots() {
    return selectedSlots;
  }

  public Long getSampledCommandSelectedSlotsUs() {
    return sampledCommandSelectedSlotsUs;
  }

  public Long getAllCommandsSelectedSlotsUs() {
    return allCommandsSelectedSlotsUs;
  }

  public long getAllCommandsAllSlotsUs() {
    return allCommandsAllSlotsUs;
  }

  public Long getNetBytesSampledCommandsSelectedSlots() {
    return netBytesSampledCommandsSelectedSlots;
  }

  public Long getNetBytesAllCommandsSelectedSlots() {
    return netBytesAllCommandsSelectedSlots;
  }

  public long getNetBytesAllCommandsAllSlots() {
    return netBytesAllCommandsAllSlots;
  }

  public long getCollectionStartTimeUnixMs() {
    return collectionStartTimeUnixMs;
  }

  public long getCollectionDurationMs() {
    return collectionDurationMs;
  }

  public long getTotalCpuTimeUserMs() {
    return totalCpuTimeUserMs;
  }

  public long getTotalCpuTimeSysMs() {
    return totalCpuTimeSysMs;
  }

  public long getTotalNetBytes() {
    return totalNetBytes;
  }

  public Map<String, Long> getByCpuTimeUs() {
    return byCpuTimeUs;
  }

  public Map<String, Long> getByNetBytes() {
    return byNetBytes;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Long> parseKeyValueMap(Object data) {
    if (data == null) {
      return Collections.emptyMap();
    }
    List<?> list = (List<?>) data;
    if (list.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Long> result = new LinkedHashMap<>();
    if (list.get(0) instanceof KeyValue) {
      for (KeyValue<?, ?> kv : (List<KeyValue<?, ?>>) list) {
        result.put(STRING.build(kv.getKey()), LONG.build(kv.getValue()));
      }
    } else {
      // RESP2 format: alternating key-value pairs
      for (int i = 0; i < list.size(); i += 2) {
        result.put(STRING.build(list.get(i)), LONG.build(list.get(i + 1)));
      }
    }
    return result;
  }

  /**
   * Parse selected-slots which is an array of [start, end] ranges. Example: [[0, 16383]] means all
   * slots.
   */
  @SuppressWarnings("unchecked")
  private static List<int[]> parseSlotRanges(Object data) {
    if (data == null) {
      return Collections.emptyList();
    }
    List<?> list = (List<?>) data;
    if (list.isEmpty()) {
      return Collections.emptyList();
    }
    List<int[]> result = new java.util.ArrayList<>(list.size());
    for (Object item : list) {
      if (item instanceof List) {
        List<?> range = (List<?>) item;
        if (range.size() == 2) {
          int start = LONG.build(range.get(0)).intValue();
          int end = LONG.build(range.get(1)).intValue();
          result.add(new int[] { start, end });
        }
      }
    }
    return result;
  }

  public static final Builder<HotkeysInfo> HOTKEYS_INFO_BUILDER = new Builder<HotkeysInfo>() {
    @Override
    @SuppressWarnings("unchecked")
    public HotkeysInfo build(Object data) {
      if (data == null) {
        return null;
      }

      List<?> list = (List<?>) data;
      if (list.isEmpty()) {
        return null;
      }

      // Check if the response is wrapped in an outer array (single element that is a List)
      // This happens when Redis returns [[key1, val1, key2, val2, ...]]
      if (list.size() == 1 && list.get(0) instanceof List) {
        list = (List<?>) list.get(0);
        if (list.isEmpty()) {
          return null;
        }
      }

      boolean trackingActive = false;
      long sampleRatio = 1;
      List<int[]> selectedSlots = Collections.emptyList();
      Long sampledCommandSelectedSlotsUs = null;
      Long allCommandsSelectedSlotsUs = null;
      long allCommandsAllSlotsUs = 0;
      Long netBytesSampledCommandsSelectedSlots = null;
      Long netBytesAllCommandsSelectedSlots = null;
      long netBytesAllCommandsAllSlots = 0;
      long collectionStartTimeUnixMs = 0;
      long collectionDurationMs = 0;
      long totalCpuTimeUserMs = 0;
      long totalCpuTimeSysMs = 0;
      long totalNetBytes = 0;
      Map<String, Long> byCpuTimeUs = Collections.emptyMap();
      Map<String, Long> byNetBytes = Collections.emptyMap();

      if (list.get(0) instanceof KeyValue) {
        // RESP3 format
        for (KeyValue<?, ?> kv : (List<KeyValue<?, ?>>) list) {
          String key = STRING.build(kv.getKey());
          Object value = kv.getValue();
          switch (key) {
            case TRACKING_ACTIVE:
              trackingActive = LONG.build(value) == 1;
              break;
            case SAMPLE_RATIO:
              sampleRatio = LONG.build(value);
              break;
            case SELECTED_SLOTS:
              selectedSlots = parseSlotRanges(value);
              break;
            case SAMPLED_COMMAND_SELECTED_SLOTS_US:
              sampledCommandSelectedSlotsUs = LONG.build(value);
              break;
            case ALL_COMMANDS_SELECTED_SLOTS_US:
              allCommandsSelectedSlotsUs = LONG.build(value);
              break;
            case ALL_COMMANDS_ALL_SLOTS_US:
              allCommandsAllSlotsUs = LONG.build(value);
              break;
            case NET_BYTES_SAMPLED_COMMANDS_SELECTED_SLOTS:
              netBytesSampledCommandsSelectedSlots = LONG.build(value);
              break;
            case NET_BYTES_ALL_COMMANDS_SELECTED_SLOTS:
              netBytesAllCommandsSelectedSlots = LONG.build(value);
              break;
            case NET_BYTES_ALL_COMMANDS_ALL_SLOTS:
              netBytesAllCommandsAllSlots = LONG.build(value);
              break;
            case COLLECTION_START_TIME_UNIX_MS:
              collectionStartTimeUnixMs = LONG.build(value);
              break;
            case COLLECTION_DURATION_MS:
              collectionDurationMs = LONG.build(value);
              break;
            case TOTAL_CPU_TIME_USER_MS:
              totalCpuTimeUserMs = LONG.build(value);
              break;
            case TOTAL_CPU_TIME_SYS_MS:
              totalCpuTimeSysMs = LONG.build(value);
              break;
            case TOTAL_NET_BYTES:
              totalNetBytes = LONG.build(value);
              break;
            case BY_CPU_TIME_US:
              byCpuTimeUs = parseKeyValueMap(value);
              break;
            case BY_NET_BYTES:
              byNetBytes = parseKeyValueMap(value);
              break;
          }
        }
      } else {
        // RESP2 format: alternating key-value pairs
        for (int i = 0; i < list.size(); i += 2) {
          String key = STRING.build(list.get(i));
          Object value = list.get(i + 1);
          switch (key) {
            case TRACKING_ACTIVE:
              trackingActive = LONG.build(value) == 1;
              break;
            case SAMPLE_RATIO:
              sampleRatio = LONG.build(value);
              break;
            case SELECTED_SLOTS:
              selectedSlots = parseSlotRanges(value);
              break;
            case SAMPLED_COMMAND_SELECTED_SLOTS_US:
              sampledCommandSelectedSlotsUs = LONG.build(value);
              break;
            case ALL_COMMANDS_SELECTED_SLOTS_US:
              allCommandsSelectedSlotsUs = LONG.build(value);
              break;
            case ALL_COMMANDS_ALL_SLOTS_US:
              allCommandsAllSlotsUs = LONG.build(value);
              break;
            case NET_BYTES_SAMPLED_COMMANDS_SELECTED_SLOTS:
              netBytesSampledCommandsSelectedSlots = LONG.build(value);
              break;
            case NET_BYTES_ALL_COMMANDS_SELECTED_SLOTS:
              netBytesAllCommandsSelectedSlots = LONG.build(value);
              break;
            case NET_BYTES_ALL_COMMANDS_ALL_SLOTS:
              netBytesAllCommandsAllSlots = LONG.build(value);
              break;
            case COLLECTION_START_TIME_UNIX_MS:
              collectionStartTimeUnixMs = LONG.build(value);
              break;
            case COLLECTION_DURATION_MS:
              collectionDurationMs = LONG.build(value);
              break;
            case TOTAL_CPU_TIME_USER_MS:
              totalCpuTimeUserMs = LONG.build(value);
              break;
            case TOTAL_CPU_TIME_SYS_MS:
              totalCpuTimeSysMs = LONG.build(value);
              break;
            case TOTAL_NET_BYTES:
              totalNetBytes = LONG.build(value);
              break;
            case BY_CPU_TIME_US:
              byCpuTimeUs = parseKeyValueMap(value);
              break;
            case BY_NET_BYTES:
              byNetBytes = parseKeyValueMap(value);
              break;
          }
        }
      }

      return new HotkeysInfo(trackingActive, sampleRatio, selectedSlots,
          sampledCommandSelectedSlotsUs, allCommandsSelectedSlotsUs, allCommandsAllSlotsUs,
          netBytesSampledCommandsSelectedSlots, netBytesAllCommandsSelectedSlots,
          netBytesAllCommandsAllSlots, collectionStartTimeUnixMs, collectionDurationMs,
          totalCpuTimeUserMs, totalCpuTimeSysMs, totalNetBytes, byCpuTimeUs, byNetBytes);
    }
  };
}
