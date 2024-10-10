package redis.clients.jedis.csc;

import java.util.Arrays;

class RedisVersion implements Comparable<RedisVersion> {

    private String version;
    private Integer[] numbers;

    public RedisVersion(String version) {
        if (version == null) throw new IllegalArgumentException("Version can not be null");
        this.version = version;
        this.numbers = Arrays.stream(version.split("\\.")).map(n -> Integer.parseInt(n)).toArray(Integer[]::new);
    }

    @Override
    public int compareTo(RedisVersion other) {
        int max = Math.max(this.numbers.length, other.numbers.length);
        for (int i = 0; i < max; i++) {
            int thisNumber = this.numbers.length > i ? this.numbers[i]:0;
            int otherNumber = other.numbers.length > i ? other.numbers[i]:0;
            if (thisNumber < otherNumber) return -1;
            if (thisNumber > otherNumber) return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return this.version;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that == null) return false;
        if (this.getClass() != that.getClass()) return false;
        return this.compareTo((RedisVersion) that) == 0;
    }

}
