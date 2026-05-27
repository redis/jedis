window.BENCHMARK_DATA = {
  "lastUpdate": 1779902782251,
  "repoUrl": "https://github.com/redis/jedis",
  "entries": {
    "Benchmark": [
      {
        "commit": {
          "author": {
            "name": "Ivo Gaydazhiev",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "01011c0060301add7a39537feaa53a135ee49cf1",
          "message": "Add JMH benchmarks (CAE-2930) (#4523)\n\n* Add JMH benchmarks for protocol reads with baseline/cache-aware comparisons\n\n* actions/cache@v5\n\n* bump actions\n\n* enable auto push\n\n* recreate Redis stream before each benchmark\n\n* push benchmark data to dedicated branch\n\n* docs: include benchmarks dashboard and add manual dispatch with deploy toggle\n\n* migrate CRC16Benchmark to JMH\n\n* add manula JMH runner\n\n* migrate SafeEncoderBenchmark\n\n* add README\n\n* migrate JedisGetSetBenchmark\n\n* unify default CI and manual runner\n\n* bootstrap redis test env\n\n* migrate RedisClientBenchmark.java\nmigrate PipelinedGetSetBenchmark.java\n\n* remove already migrated ProtocolBenchmark.java\n\n* add  GetSetMixedR90W10Benchmarks\n  Workload benchmark: mixed GET/SET at 90% read / 10% write.\n  Provides a single comparable throughput number per client configuration:\n\n* drop RedisClientCSCBenchmark covered by GetSetMixedR90W10 workload benchmark\n\n* migrate PoolBenchmark.java\n\n* test(jmh): add PubSubPushBenchmark for end-to-end publish→onMessage round-trip throughput\n\n* address review comment for overflow\n\n* format\n\n* clean up\n\n* remove unnecessary JVM_OPTS: -Xmx3200m\n\n* silence RESP3 protocol warning\n\n* address review comment - Integer counter overflow\n\n* pin test server version to 8.2.6\n\n* keep old benchmarks for comparison with previous versions\n\nclean up\n\n* simplify SafeEncodeBenchmark use single string\n\n* format SafeEncodeBenchmark\n\n* address Level.Invocation unsuitable for nanosecond-scale protocol benchmarks\n\n* format\n\n* enforce checkout & build docs from master\n\n* Use distinct per-thread seeds in GetSetMixedR90W10Benchmark Rng\n\nAll threads shared the same SEED, generating identical key-index sequences\nand forcing T8 variants into lockstep on a single key per iteration. Derive\neach thread's seed from SEED + an AtomicInteger counter; per-run\nreproducibility is preserved (counter resets per fork).\n\n* Migrate ReadBenchmark/ReadPushesBenchmark to PushConsumerChain API\n\nProtocol.read/readPushes no longer accept a Cache directly; the\ncache-as-push-consumer role moved into PushConsumerChain. Wrap the\nexisting Cache in PushInvalidateConsumer + PushConsumerChainImpl so the\nbenchmarks still measure the realistic CSC read path.",
          "timestamp": "2026-05-27T17:03:42Z",
          "url": "https://github.com/redis/jedis/commit/01011c0060301add7a39537feaa53a135ee49cf1"
        },
        "date": 1779902781680,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 8056.546354283719,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 535164.5863470417,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 471035.1469253716,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7999.631249779879,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5441.040084591415,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 8219.717192772163,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 537135.3987791637,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 468040.5463164585,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7949.828419970993,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48511.93062032627,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1685822.0757350374,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1208602.3320639182,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 48065.20264127797,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 32477.43149208776,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1362845.9158334206,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1033771.7698493975,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 32120.263530443084,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7933.983501786604,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 32615.13005114888,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 8058.624603004677,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12959.54201037847,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 44088.38894909888,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 8038.446548128677,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 32358.117818460836,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 110.22314434359669,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.23716183905443,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 153.8478540160519,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.733550650640144,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 109.29391322317679,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 32.87600233565479,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 145.94606459056564,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.051668401662615,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21891.61118143295,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 274.2042107461866,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 223185.68097777775,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 232.42082750768213,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 101.82761483097426,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.31017490517214,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 98.23715835517062,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.70569447382155,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.97669375889072,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}