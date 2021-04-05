package redis.clients.jedis;

import java.io.Serializable;
import java.util.List;

public class StreamAutoClaim implements Serializable {

  private static final long serialVersionUID = 1L;

  private StreamEntryID id;
  private List<StreamEntry> streamEntries;

  public StreamAutoClaim(StreamEntryID id, List<StreamEntry> streamEntries) {
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
