package redis.clients.jedis;

public class PendingEntry {
  final private EntryID id;
  final private String consumerName;
  final private long idleTime;
  final private long deliveredTimes;
  
  public PendingEntry(EntryID id, String consumerName, long idleTime, long deliveredTimes) {
    this.id = id;
    this.consumerName = consumerName;
    this.idleTime = idleTime;
    this.deliveredTimes = deliveredTimes;
  }
  
  public EntryID getID() {
    return id;
  }

  public long getIdleTime() {
    return idleTime;
  }

  public long getDeliveredTimes() {
    return deliveredTimes;
  }

  public String getConsumerName() {
    return consumerName;
  }

}
