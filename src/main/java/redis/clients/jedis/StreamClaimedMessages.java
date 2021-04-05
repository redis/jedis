package redis.clients.jedis;

import java.io.Serializable;
import java.util.List;

public class StreamClaimedMessages implements Serializable {

  private static final long serialVersionUID = 1L;

  private final StreamEntryID id;
  private final List<StreamEntry> streamEntries;

  public StreamClaimedMessages(StreamEntryID id, List<StreamEntry> streamEntries) {
    this.id = id;
    this.streamEntries = streamEntries;
  }

  public StreamEntryID getId() {
    return id;
  }

  public List<StreamEntry> getStreamEntries() {
    return streamEntries;
  }

}
