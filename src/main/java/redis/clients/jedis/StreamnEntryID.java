package redis.clients.jedis;

import java.io.IOException;
import java.io.Serializable;

public class StreamnEntryID implements Comparable<StreamnEntryID>, Serializable{
  
  private static final long serialVersionUID = 1L;

  /**
  * Should be used only with XADD  
  * 
  * <code>
  * XADD mystream * field1 value1 
  * </code>
  */
  public static final StreamnEntryID NEW_ENTRY = new StreamnEntryID() {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public String toString(){
      return "*";
    }
  };
  
  
  /**
  * Should be used only with XGROUP CREATE  
  * 
  * <code>
  * XGROUP CREATE mystream consumer-group-name $
  * </code>
  */
  public static final StreamnEntryID LAST_ENTRY = new StreamnEntryID() {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public String toString(){
      return "$";
    }
  };
  
  /**
   * Should be used only with XREADGROUP
   * <code>
   * XREADGROUP $GroupName $ConsumerName BLOCK 2000 COUNT 10 STREAMS mystream >
   * </code>
   */
  public static final StreamnEntryID UNRECEIVED_ENTRY = new StreamnEntryID() {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public String toString(){
      return ">";
    }
  };
    
  private long time;
  private long sequence;

  public StreamnEntryID() {
    this(0, 0L);
  }
  
  public StreamnEntryID(String id) {
    String[] split = id.split("-");    
    this.time = Long.parseLong(split[0]);
    this.sequence = Long.parseLong(split[1]);
  }
  
  public StreamnEntryID(long time, long sequence) {
    this.time = time;
    this.sequence = sequence;
  }

  @Override
  public String toString() {
    return time + "-" + sequence;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    StreamnEntryID other = (StreamnEntryID) obj;
    return this.time == other.time && this.sequence == other.sequence;
  }
  
  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public int compareTo(StreamnEntryID other) {
    int timeComapre = Long.compare(this.time, other.time);
    return timeComapre != 0 ? timeComapre : Long.compare(this.sequence, other.sequence);
  }

  public long getTime() {
    return time;
  }

  public long getSequence() {
    return sequence;
  }
  
  private void writeObject(java.io.ObjectOutputStream out) throws IOException{
    out.writeLong(this.time);
    out.writeLong(this.sequence);
  }
  
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
    this.time = in.readLong();
    this.sequence = in.readLong();
  }
}
