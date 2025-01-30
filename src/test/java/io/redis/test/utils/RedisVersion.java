package io.redis.test.utils;

public class RedisVersion implements Comparable<RedisVersion> {
    public static final RedisVersion V6_0_0 = RedisVersion.of("6.0.0");
    public static final RedisVersion V7_0_0 = RedisVersion.of("7.0.0");
    public static final RedisVersion V7_2_0 = RedisVersion.of("7.2.0");
    public static final RedisVersion V7_4 = RedisVersion.of("7.4");
    public static final RedisVersion V8_0_0_PRE = RedisVersion.of("7.9.0");
    public static final RedisVersion V8_0_0 = RedisVersion.of("8.0.0");

    private final int major;
    private final int minor;
    private final int patch;

    // Private constructor to enforce use of the static factory method
    private RedisVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    // Static method to create a RedisVersion from a version string
    public static RedisVersion of(String version) {
        // Check for the "redis_version:" prefix and remove it if present
        if (version.startsWith("redis_version:")) {
            version = version.substring("redis_version:".length());
        }

        // Split the version string by the '.' character
        String[] parts = version.split("\\.");

        // Parse each component, setting defaults for missing parts
        int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

        return new RedisVersion(major, minor, patch);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    @Override
    public String toString() {
        return "RedisVersion{" +
                "major=" + major +
                ", minor=" + minor +
                ", patch=" + patch +
                '}';
    }

    @Override
    public int compareTo(RedisVersion other) {
        // Compare major, minor, and patch versions
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    public boolean isLessThanOrEqualTo(RedisVersion other) {
        return this.compareTo(other) <= 0;
    }

    public boolean isLessThan(RedisVersion other) {
        return this.compareTo(other) < 0;
    }

    public boolean isGreaterThanOrEqualTo(RedisVersion other) {
        return this.compareTo(other) >= 0;
    }

    public boolean isGreaterThan(RedisVersion other) {
        return this.compareTo(other) > 0;
    }

    // Static method to compare two RedisVersion instances
    public static int compare(RedisVersion v1, RedisVersion v2) {
        return v1.compareTo(v2);
    }
}