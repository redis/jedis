# Jedis JMH Benchmarks

JMH (Java Microbenchmark Harness) benchmarks for Jedis performance testing.

## Quick Start

### Maven (CI/CD)
```bash
mvn -Pjmh clean test                              # All benchmarks
mvn -Pjmh test -Djmh.includes="CRC16Benchmark"   # Specific suite
```

### IDE (Development)
1. Open `JmhMain.java`
2. Uncomment desired suite: `runAllBenchmarks()`, `runCRC16Benchmarks()`, etc.
3. Right-click → Run

### Results
- `benchmarks.json` - Machine-readable results
- `benchmark.log` - Execution log

## Available Benchmarks

### ProtocolReadBenchmark (14 benchmarks)
RESP3 protocol operations with client-side caching support.

**Categories:**
- Baseline protocol: `readSimpleString`, `readBulkString`, `readArray`, etc.
- Cache-aware overhead: Same operations with cache enabled
- Push processing: `processSinglePushInvalidation`, `processLargePushInvalidation`
- Mixed scenarios: `readWith1/10/100PushMessages` (realistic workload)

**Use case:** Measure client-side caching impact and push notification overhead.

### CRC16Benchmark (2 benchmarks)
Redis Cluster hash slot calculation.

**Methods:**
- `getSlotString` - String-based calculation
- `getSlotBytes` - byte[]-based calculation

**Test data:** Rotates through 8 key patterns (empty, short, long, hashtag, etc.)
**Batch size:** 128 ops (8 patterns × 16)

### SafeEncoderBenchmark (3 benchmarks)
UTF-8 encoding/decoding performance.

**Methods:**
- `encodeStringToBytes` - String → byte[]
- `decodeBytesToString` - byte[] → String
- `encodeMany` - Bulk encoding

**Test data:** Rotates through 6 string patterns (2-44 chars)
**Batch size:** 120 ops (6 patterns × 20)

### JedisGetSetBenchmark (2 benchmarks) ⚠️ Requires Redis
Real Jedis GET/SET operations over network.

**Methods:**
- `set` - Write performance (foo=bar)
- `get` - Read performance (foo=bar)

**Requirements:** Redis 6.0+ running on localhost:6379 (or configured endpoint)
**Mode:** Throughput (ops/sec)

---

## Configuration

**JMH Settings:**
- Forks: 1
- Warmup: 3 iterations × 1s
- Measurement: 5 iterations × 1s
- Output: Average time (ns/op)

## Understanding Results

```
Benchmark                        Mode  Cnt   Score   Error  Units
CRC16Benchmark.getSlotString     avgt    5  47.123 ± 2.456  ns/op
```

- **Score**: Average time per operation (lower = better)
- **Error**: 99.9% confidence interval (< 10% = good)
- **Units**: ns/op (nanoseconds per operation)

**Good result:** Error < 10% of score
**High variance:** Error > 20% of score (need more iterations or batching)

---

## Notes

- **Batching:** Fast operations (< 100 ns) use batching to reduce JMH overhead
- **Test patterns:** Batch size is always a multiple of pattern count for consistent results
- **IDE runner:** `JmhMain.java` provides easy IDE execution
- **Redis required:** JedisGetSetBenchmark requires a running Redis instance (localhost:6379)
- **CI/CD:** GitHub Actions runs benchmarks nightly, results published to gh-pages
