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

### protocol.ReadBenchmark (10 benchmarks)
RESP3 `Protocol.read` operations with optional client-side cache.

**Categories:**
- Baseline reads (no cache): `readSimpleString`, `readBulkString`, `readArray`, `readMultiBulkResponse`
- Cache-aware overhead: `cacheAwareRead*` variants (push-check path, no pushes present)
- Mixed scenarios: `readWith1PushMessage`, `readWith100PushMessages` (push frames preceding a response)

**Use case:** Measure RESP parsing cost and the overhead introduced by client-side caching.

### protocol.ReadPushesBenchmark (2 benchmarks)
`Protocol.readPushes` — client-side cache invalidation processing.

**Methods:**
- `drain1Pending` - One `readPushes` call drains 1 pending push frame
- `drain1000Pending` - One `readPushes` call drains 1000 pending push frames

**Use case:** Measure how `readPushes` per-call cost scales with burst size; reveals fixed-vs-amortized overhead.

### protocol.SendCommandBenchmark (1 benchmark)
`Protocol.sendCommand` — RESP command encoding.

**Methods:**
- `measureSendCommand` - Encode SET command (full path: `CommandArguments` + encode)

**Use case:** Measure command-encoding cost without I/O.

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

### jedis.GetSetBenchmark (4 benchmarks) ⚠️ Requires Redis
Jedis GET/SET operations over network.

**Methods:**
- `set` - Write performance (foo=bar)
- `get` - Read performance (foo=bar)
- `pipelinedSet` - Pipelined write (1000 ops, sync every 100)
- `pipelinedGet` - Pipelined read (1000 ops, sync every 100)

**Requirements:** Redis 6.0+ running on localhost:6379 (or configured endpoint)
**Mode:** Throughput (ops/sec)

### redisclient.GetSetBenchmark (12 benchmarks) ⚠️ Requires Redis
RedisClient (pooled) GET/SET operations with concurrency testing.

**Thread configurations:**
- `redisclient.GetSetBenchmark$Threads1` - 1 thread (baseline)
- `redisclient.GetSetBenchmark$Threads8` - 8 threads (moderate concurrency)
- `redisclient.GetSetBenchmark$Threads64` - 64 threads (high concurrency)

**Methods per configuration:**
- `set` - Write performance using pooled RedisClient
- `get` - Read performance using pooled RedisClient
- `pipelinedSet` - Pipelined write (1000 ops, sync every 100)
- `pipelinedGet` - Pipelined read (1000 ops, sync every 100)

**Pool configuration:**
- MaxTotal: 64 connections (sized for max thread count)
- MaxIdle: 64, MinIdle: 8
- Prevents pool contention from skewing results

**Workload:**
- Same single key/value pair (`"foo"`/`"bar"`) as `jedis.GetSetBenchmark` for direct comparability
- 1-thread variant produces numbers comparable to `jedis.GetSetBenchmark`
- 8/64-thread variants exercise pool concurrency on the same workload

**Requirements:** Redis 6.0+ running on localhost:6379 (or configured endpoint)
**Mode:** Throughput (ops/sec)
**Purpose:** Test connection pool efficiency and pipelining under different load levels

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
- **Redis required:** `jedis.GetSetBenchmark` and `redisclient.GetSetBenchmark` require a running Redis instance (localhost:6379)
- **CI/CD:** GitHub Actions runs benchmarks nightly, results published to gh-pages
