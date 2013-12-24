package redis.clients.util;

public abstract class ShardInfo<T> {
    private int weight;

    public ShardInfo() {
    }

    public ShardInfo(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return this.weight;
    }

    protected abstract T createResource();
    
    public abstract String getName();
   
    @Override 
    public String toString() {
	    return getName() + "*" + getWeight();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ShardInfo<T> shardInfo = (ShardInfo<T>)obj;
        return weight == shardInfo.weight && getName().equals(shardInfo.getName());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
