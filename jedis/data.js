window.BENCHMARK_DATA = {
  "lastUpdate": 1782439344861,
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
      },
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
        "date": 1779904489824,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11132.790154521841,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 684152.2208255769,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 600509.2711432647,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11118.800174972108,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6999.974619416427,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 10926.110384466752,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 692276.3129627153,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 595136.1200461359,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11049.765700885357,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54630.51773169475,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1868132.1034370712,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1391595.7633218775,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 53859.93175212594,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37820.31090076421,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1610305.6795380183,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1218979.0142394681,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37622.6730537152,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10969.870492414324,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37039.39813037661,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11175.755810322526,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17229.86361497976,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 54168.01525177508,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11000.731913887612,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36957.51636385121,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.31861793431445,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.49799403034853,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.56015053379474,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.589573722980887,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 84.94580494440339,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.47723407598611,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 133.07764254489956,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.533708174758294,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21772.13701650859,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 215.9424266687876,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 194334.4107682972,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 237.90117566883868,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.18195586126781,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 38.60580812456796,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.96323831824161,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.353625682903083,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 37.15782764177116,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "committer": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "id": "53da4a4f7dd179d0c748f8847ae1960c6086408d",
          "message": "docs workflow: trigger via workflow_run after Benchmarks complete",
          "timestamp": "2026-05-27T18:05:24Z",
          "url": "https://github.com/redis/jedis/commit/53da4a4f7dd179d0c748f8847ae1960c6086408d"
        },
        "date": 1779908173164,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11307.979805759485,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 687046.3433804179,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 609989.1669710771,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11142.453321900688,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7044.620256331128,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11212.113921907126,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 696967.7930978474,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 610369.4955310678,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10990.03300404246,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54397.80938737593,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1871965.309973798,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1406722.3151227706,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52925.2171384321,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38153.153872437295,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1646619.413318967,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1214305.5726242214,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37282.31028478001,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10979.019703988966,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37633.73868682172,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11130.986033270096,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17417.073860793884,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 51236.79579380696,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11042.769246211978,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 38002.61828811849,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.45348133594156,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.362790233845296,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 116.13530006349603,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.501459845718742,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 83.63299198431295,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.327305657077332,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 108.59362047442073,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.50669315186729,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18545.69314576806,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 228.26130868489525,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 189389.8960943396,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 199.50018245262754,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.88172715850718,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.064553618996676,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.59823164943991,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.408908711032534,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.17228262494517,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "committer": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "id": "53da4a4f7dd179d0c748f8847ae1960c6086408d",
          "message": "docs workflow: trigger via workflow_run after Benchmarks complete",
          "timestamp": "2026-05-27T18:05:24Z",
          "url": "https://github.com/redis/jedis/commit/53da4a4f7dd179d0c748f8847ae1960c6086408d"
        },
        "date": 1779933282109,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7933.822472379523,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 538273.4077365801,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 473338.0756684823,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7967.697147138866,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5411.340389212145,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7874.101720319637,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 540530.2818349653,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 470662.23064658884,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7853.482236265641,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48335.84658464404,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1665094.0757683513,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1199063.8854509345,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47587.5510189602,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 43382.87336312902,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1362316.8370437564,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1011887.7848416741,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31045.653014189396,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7931.75532255955,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 35586.3963503118,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7956.51541201772,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12712.663940286566,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42348.95412403424,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7996.67137751892,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 32035.076154686227,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 117.36599391204436,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.326780855461358,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 148.1110391555393,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.61756970352638,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 108.283057092834,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.24004546119022,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 145.7531580171868,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.35965266814286,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21708.503919597668,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 260.38799969697845,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 222740.50642930405,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 237.44567932026052,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 100.33953887987985,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.34892432477498,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 98.3481313877135,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.777549019646635,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.6276931620033,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "committer": {
            "name": "ggivo",
            "username": "ggivo",
            "email": "ivo.gaydazhiev@redis.com"
          },
          "id": "53da4a4f7dd179d0c748f8847ae1960c6086408d",
          "message": "docs workflow: trigger via workflow_run after Benchmarks complete",
          "timestamp": "2026-05-27T18:05:24Z",
          "url": "https://github.com/redis/jedis/commit/53da4a4f7dd179d0c748f8847ae1960c6086408d"
        },
        "date": 1780019909897,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 8927.233327688326,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 582039.5176836995,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 508874.8553789174,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 8814.871659459051,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5976.257107657954,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 8852.18598109259,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 582741.1268532828,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 513163.56903893966,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 8712.423813307445,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 51366.80816501244,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1707554.6081176132,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1269099.023337247,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 49262.61082400698,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 34957.64227570338,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1426527.951738296,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1076094.0698311718,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 32612.8356084765,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 8738.550724341349,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 32559.123703182395,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 8859.988127767663,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 14134.943671598321,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 44431.65987123176,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 8723.828920655773,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 32826.47836952162,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 115.67972031357733,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 32.669271333079834,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 146.48135265846716,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.642674852904275,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 110.99606710490077,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.231199077795804,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 173.83584774167304,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.635132511000368,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22544.451049402425,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 267.28086471177926,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 224632.93527116106,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 232.09857008548633,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 100.75076728345371,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 35.9326615487255,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 97.74480233930262,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.691337581811396,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 47.110264621734764,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "490575c71a4038f656a4a2b87ba4cc6dd75898ac",
          "message": "Fix connection leak in MultiDbConnectionSupplier (#4546)\n\n* Fix connection leak in MultiDbConnectionSupplier\n\n* Remove fqn import\n\n* Use validated flag to close connection",
          "timestamp": "2026-05-29T10:38:57Z",
          "url": "https://github.com/redis/jedis/commit/490575c71a4038f656a4a2b87ba4cc6dd75898ac"
        },
        "date": 1780106166885,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 14344.759835263143,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 864425.3164233889,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 760066.7123999508,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 14204.246594874785,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 9529.957846630903,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 14202.353376228659,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 871160.5434692556,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 762909.7586113919,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 13763.04255957524,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 71925.45021590093,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 2373797.269600574,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1809008.5988629158,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 66440.27356761468,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 48098.64662074359,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 2109172.183904414,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1598175.9356914556,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 48017.15661221715,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 13978.184462552677,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 47744.743784653416,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 14134.508327776848,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 22059.510454333817,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 64472.30822360339,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 13910.037832649052,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 48780.2803530431,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 63.95074852987328,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 16.623777774461395,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 86.4829142506763,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 11.285978542491616,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 65.88473338724792,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 16.65440678947008,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 84.6806686029789,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 11.293140317154991,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 13724.484791529976,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 173.85069345970655,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 146608.30217518247,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 156.17962112127452,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 87.26173639241831,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 30.33148480522512,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 63.44604229375307,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 23.741841704579024,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 26.656249854897688,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "490575c71a4038f656a4a2b87ba4cc6dd75898ac",
          "message": "Fix connection leak in MultiDbConnectionSupplier (#4546)\n\n* Fix connection leak in MultiDbConnectionSupplier\n\n* Remove fqn import\n\n* Use validated flag to close connection",
          "timestamp": "2026-05-29T10:38:57Z",
          "url": "https://github.com/redis/jedis/commit/490575c71a4038f656a4a2b87ba4cc6dd75898ac"
        },
        "date": 1780192925831,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11594.011306945393,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 685882.2795433986,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 607444.1772627821,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11200.898432061835,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7434.720662033188,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11183.632912508267,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 689813.3341429115,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 610579.3074438935,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11005.444744326835,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55166.168405951175,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1844380.949813937,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1408396.5819906688,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 53680.53722537274,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38524.41529693386,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1609091.9606035724,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1216766.7667738728,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36979.19743449871,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11061.724923894992,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37207.55276091366,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11169.55204601556,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17175.155565268455,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50668.72203111119,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11026.309907537083,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36901.09561728418,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.71922376262276,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 25.29247533744164,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 109.03709760076909,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.528806501233655,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.95278088528946,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.289098017898628,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 109.13525141824773,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.610192243040197,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 19576.113928664825,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 244.32095636199384,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 189624.9544528302,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 238.33585972796254,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.52844572268532,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.12645138613234,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 79.82831442889193,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.46199890453051,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.049104227728456,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "490575c71a4038f656a4a2b87ba4cc6dd75898ac",
          "message": "Fix connection leak in MultiDbConnectionSupplier (#4546)\n\n* Fix connection leak in MultiDbConnectionSupplier\n\n* Remove fqn import\n\n* Use validated flag to close connection",
          "timestamp": "2026-05-29T10:38:57Z",
          "url": "https://github.com/redis/jedis/commit/490575c71a4038f656a4a2b87ba4cc6dd75898ac"
        },
        "date": 1780279518796,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11400.54021203726,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 687023.4456481861,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 602424.805599154,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11540.069705221173,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7114.1337591883685,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11208.422810530887,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 694233.9420709348,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 613857.2875653559,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11291.280538679377,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55187.58268155678,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1885735.0775707953,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1411531.5733649596,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 54320.35836936933,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38669.089999959266,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1648334.4386695474,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1231202.8613348303,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 38312.998574108045,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11055.354720707333,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37044.039448293435,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11225.115780811324,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17212.75717605727,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 51739.0788111041,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10964.936421802951,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37371.33710735575,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 84.48913897260402,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.48517261328434,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 113.07167544991549,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.552693657892613,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 85.97935506785934,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.314637082903854,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.73391192202546,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.730165174734992,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 19700.131207232604,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 223.5043116278015,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 189941.5447015274,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 202.80788284370723,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.00465561439701,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.081689693584785,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 79.56346895740396,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.342382669219273,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.18344284472717,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "2647354f265ee11784aff8bd8b8162c1ed63978f",
          "message": "Bump maven.surefire.version from 3.5.5 to 3.5.6 (#4554)\n\nBumps `maven.surefire.version` from 3.5.5 to 3.5.6.\n\nUpdates `org.apache.maven.plugins:maven-surefire-plugin` from 3.5.5 to 3.5.6\n- [Release notes](https://github.com/apache/maven-surefire/releases)\n- [Commits](https://github.com/apache/maven-surefire/compare/surefire-3.5.5...surefire-3.5.6)\n\nUpdates `org.apache.maven.plugins:maven-failsafe-plugin` from 3.5.5 to 3.5.6\n- [Release notes](https://github.com/apache/maven-surefire/releases)\n- [Commits](https://github.com/apache/maven-surefire/compare/surefire-3.5.5...surefire-3.5.6)\n\n---\nupdated-dependencies:\n- dependency-name: org.apache.maven.plugins:maven-surefire-plugin\n  dependency-version: 3.5.6\n  dependency-type: direct:production\n  update-type: version-update:semver-patch\n- dependency-name: org.apache.maven.plugins:maven-failsafe-plugin\n  dependency-version: 3.5.6\n  dependency-type: direct:production\n  update-type: version-update:semver-patch\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-06-01T08:07:27Z",
          "url": "https://github.com/redis/jedis/commit/2647354f265ee11784aff8bd8b8162c1ed63978f"
        },
        "date": 1780366135622,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7913.642149768985,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 531077.9423617024,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 464940.1445690772,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7957.454642567553,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5335.2068279270825,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7945.017021127152,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 529492.6101589685,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 467869.3562045231,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7886.976164578288,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 47783.159250298166,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1677563.7081242495,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1206824.283457711,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47352.42127590683,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 31651.56129791647,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1352852.0942052489,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 994880.2574969219,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31422.782257022256,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7868.06521454039,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 31182.61083387977,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7868.426345207648,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12576.654172652215,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 46201.11414691946,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7860.662320472456,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31943.970511869215,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 118.87195723040236,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.208343728146975,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 145.21687740937645,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.729804462405719,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 117.36112602744406,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 32.64592711781435,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 175.99795307401183,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.356116983160609,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22136.508238323437,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 245.44334678239994,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 230568.91719540226,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 260.7444300237863,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 100.30967932779205,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 35.95335861492898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 98.22043878076633,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.64493232606619,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.691322064695434,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "2ec0938d621aa324680490c7c20a09c27563d272",
          "message": "Bump org.codehaus.mojo:build-helper-maven-plugin from 3.5.0 to 3.6.1 (#4553)\n\nBumps [org.codehaus.mojo:build-helper-maven-plugin](https://github.com/mojohaus/build-helper-maven-plugin) from 3.5.0 to 3.6.1.\n- [Release notes](https://github.com/mojohaus/build-helper-maven-plugin/releases)\n- [Commits](https://github.com/mojohaus/build-helper-maven-plugin/compare/3.5.0...3.6.1)\n\n---\nupdated-dependencies:\n- dependency-name: org.codehaus.mojo:build-helper-maven-plugin\n  dependency-version: 3.6.1\n  dependency-type: direct:development\n  update-type: version-update:semver-minor\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-06-02T08:03:08Z",
          "url": "https://github.com/redis/jedis/commit/2ec0938d621aa324680490c7c20a09c27563d272"
        },
        "date": 1780452786272,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11312.543511326387,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 688766.864144193,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 600624.5289741041,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11183.846624742437,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7332.894190847457,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11182.770446005045,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 683906.5532597221,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 606491.883107506,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10814.64911184928,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55235.98343316316,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1888815.2771254093,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1402624.8878884723,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 51810.20281693882,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38386.73389824007,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1635128.6454458295,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1221383.5585323595,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37263.87052640307,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10921.239926026936,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37376.673762381055,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11070.383052078398,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17103.150239324652,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 51149.13446972163,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10884.483427182764,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37280.078443175786,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.66635769902184,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.436994626522253,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.81730474184447,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.551263570224872,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 84.88440142885007,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.419549785578784,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 133.42306926832623,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.545381497961893,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18517.79532322529,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 228.99430832805393,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 184375.31343395042,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 200.68257602623538,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.57400064484762,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.10879275395942,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 83.09242986493578,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.51329526215909,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.14796096223229,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Bharat Agarwal",
            "username": "agarwalbharat",
            "email": "agarwalbharat68@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "e6444fb79b368ab2e066f82387a9377dc372a184",
          "message": "Expose renewSlotCache on JedisCluster as public API (#4555)\n\nExpose renewSlotCache on JedisCluster as refreshClusterTopology\n\nAdd JedisCluster#refreshClusterTopology() as a public API that delegates to the existing ClusterConnectionProvider renewal path. This gives applications an explicit way to refresh the in-memory cluster slot cache when they detect stale topology state outside the normal command retry flow.",
          "timestamp": "2026-06-03T12:43:05Z",
          "url": "https://github.com/redis/jedis/commit/e6444fb79b368ab2e066f82387a9377dc372a184"
        },
        "date": 1780539156319,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 8978.590377636308,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 579602.5345202246,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 505082.97863690805,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 8822.507279875736,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6014.928934627735,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 8841.08109688071,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 574303.5519840948,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 502914.74827956624,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 8638.203952431602,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 50096.90011268561,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1689172.7516146365,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1230305.2424033808,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 49781.302118727224,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 33178.99096724514,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1417606.2520991885,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1068771.9082518267,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 33254.29711913172,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 8774.689519319576,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 33031.923733330405,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 8932.684341356584,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 13998.945702883697,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 45333.67116250109,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 8780.466444656528,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 33087.640097009506,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 115.34884960373795,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.18923355876641,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 147.70760633745908,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.325298089490213,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 117.12460239598167,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.3109605061381,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 147.71919520604177,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.02717792707893,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21613.41337179521,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 259.292889000661,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 224957.59202209587,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 242.22079015198648,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 107.9788922683797,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.27778138343257,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 97.9254155361716,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.64963212427931,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.92337708063953,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e",
          "message": "Upgrade GitHub Actions versions (#4558)\n\n* Upgrade GitHub Actions versions\n\n* Upgrade run-tests action too",
          "timestamp": "2026-06-04T13:47:33Z",
          "url": "https://github.com/redis/jedis/commit/33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e"
        },
        "date": 1780624971780,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11460.960035691063,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 689836.499565481,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 607723.3978343572,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11056.879528907646,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6993.724112829623,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11310.154456945264,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 694624.0059011057,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 608602.1312344985,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11048.849372518922,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 53748.343047725946,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1855937.4946212866,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1397133.657604042,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52548.1244919935,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37696.522597691466,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1627351.4823525767,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1218714.849940859,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37551.41864504789,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11066.09325662265,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37317.625105411,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11328.32260179562,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17479.189250993946,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50458.601335090425,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11045.868671981962,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36986.89467758248,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.28695721890836,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 25.26377913367918,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 115.56705639020751,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.854080162070883,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 83.74737157586647,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.467428104775827,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 108.5004213279373,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.49522998738018,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22354.163067124966,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 201.4923258381867,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 194676.5665806572,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 201.06047647655092,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.55966502432955,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.03965737549066,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.99393806907712,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.55336235297481,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.3447315322219,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e",
          "message": "Upgrade GitHub Actions versions (#4558)\n\n* Upgrade GitHub Actions versions\n\n* Upgrade run-tests action too",
          "timestamp": "2026-06-04T13:47:33Z",
          "url": "https://github.com/redis/jedis/commit/33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e"
        },
        "date": 1780711126112,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7996.177437777815,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 529582.490405507,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 468054.6491184324,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7983.625108237543,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5689.225408969549,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7925.8429170020845,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 531036.12325935,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 465292.9166399504,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7960.598699525135,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48513.17456575412,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1668249.0774661896,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1211676.020771239,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47824.654771015084,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 44971.97242824652,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1341552.8349299536,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1030439.9219817959,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31280.021978019075,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7861.922687026835,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 31381.825872312114,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7886.475564119867,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12601.89002314879,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42070.41831834721,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7978.789208616186,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31632.3042350296,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 110.33628195208753,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.28875482421566,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 174.4457936397019,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.324853419693984,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 116.99493420892277,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.371972309087447,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 153.11075068241917,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.735601129696423,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21614.505027018615,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 269.96261178101315,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 233514.74325260628,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 236.46429826194048,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 99.69545981017272,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.26126747748911,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 94.39218424487133,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.710450829550346,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.71656410652337,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e",
          "message": "Upgrade GitHub Actions versions (#4558)\n\n* Upgrade GitHub Actions versions\n\n* Upgrade run-tests action too",
          "timestamp": "2026-06-04T13:47:33Z",
          "url": "https://github.com/redis/jedis/commit/33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e"
        },
        "date": 1780797797785,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11225.968110588225,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 691365.1813050655,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 615208.9094592396,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11133.911127959378,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7511.832330774519,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11156.519971882904,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 695133.6294490279,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 609963.2178749663,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11261.934095441955,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54728.971219760366,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1864529.092230988,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1395522.1583900969,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 53841.43832769299,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37851.0331237086,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1640022.4562221344,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1233440.1294188828,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37118.2780282905,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11123.847718844472,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37567.43776059425,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11241.169206890605,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17347.91707473428,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50908.035972831996,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11069.04083422432,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37326.58722837429,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 83.56250358089807,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.433264706359633,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 111.09400948380232,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.617090662769266,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 85.439039534985,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.330107236765706,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 108.56115778999802,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.583413695847401,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 19787.16224772354,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 229.90302152705644,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 199257.11265726498,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 198.04717388713644,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.757092927664,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.0696090166431,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 84.32329399599338,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.376634302697205,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.24965009598366,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e",
          "message": "Upgrade GitHub Actions versions (#4558)\n\n* Upgrade GitHub Actions versions\n\n* Upgrade run-tests action too",
          "timestamp": "2026-06-04T13:47:33Z",
          "url": "https://github.com/redis/jedis/commit/33fc911a2aa01e93fa4bb2e0caae72e85a26ec4e"
        },
        "date": 1780884352995,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7856.492018837086,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 526938.8820059944,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 461556.9073265747,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7801.631772513138,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5743.810654456096,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7750.432412486381,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 533300.243213339,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 459565.12658690166,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7795.557127484048,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 47404.36400771714,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1639241.6837338903,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1178467.5472502671,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47251.936587986,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 31397.325795859237,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1352394.4987875125,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1014294.2399671093,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31601.530016529105,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7724.3013140453695,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 32229.94012520804,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7779.032108847705,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12240.829297670349,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 41715.68308734205,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7705.338566850134,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 30967.34242413183,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 111.01290576054153,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.213497364864878,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 146.0675505310518,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.061736592566485,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 110.7187202495383,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.263508720184667,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 153.2368027592119,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.405535893559165,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22246.380739705437,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 253.1089579411253,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 241078.71206368328,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 246.79075228326843,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 103.25662623267826,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.28319190715723,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 98.25892591214203,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.612948893730575,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.79419377168021,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "c98737cd3666551ad454597cf52c61b6124bd7bb",
          "message": "Bump org.codehaus.mojo:exec-maven-plugin from 3.1.1 to 3.6.3 (#4552)\n\nBumps [org.codehaus.mojo:exec-maven-plugin](https://github.com/mojohaus/exec-maven-plugin) from 3.1.1 to 3.6.3.\n- [Release notes](https://github.com/mojohaus/exec-maven-plugin/releases)\n- [Commits](https://github.com/mojohaus/exec-maven-plugin/compare/3.1.1...3.6.3)\n\n---\nupdated-dependencies:\n- dependency-name: org.codehaus.mojo:exec-maven-plugin\n  dependency-version: 3.6.3\n  dependency-type: direct:development\n  update-type: version-update:semver-minor\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>\nCo-authored-by: Ivo Gaydazhiev <ivo.gaydazhiev@redis.com>",
          "timestamp": "2026-06-08T13:05:40Z",
          "url": "https://github.com/redis/jedis/commit/c98737cd3666551ad454597cf52c61b6124bd7bb"
        },
        "date": 1780970197923,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11255.28905923667,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 689598.9532737856,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 601627.6312349241,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11130.080362749928,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7596.2900187552295,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11068.550399236417,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 677949.0284396902,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 593955.6013063178,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10853.699929165085,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54807.63876069883,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1865452.562489488,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1394259.2521730927,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 50994.054813975556,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38275.2136063639,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1627736.7456317903,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1234488.9655822888,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36922.987536608394,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10947.915790528152,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37033.98604102778,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11169.349415811388,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17138.76176194718,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 49743.02200138622,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11010.048743566213,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36856.80247769196,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 83.54737232382132,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 25.254711995232686,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 116.47763545721641,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.763631262037183,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 84.9886629967123,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.363727222998723,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 133.5504446893328,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.524852565826553,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18553.83124078494,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 243.35117863387418,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 189609.05316393852,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 209.42390570761077,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.02574101073738,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.878784801615055,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 83.46385874710593,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.48306205551832,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.42374228899094,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "c98737cd3666551ad454597cf52c61b6124bd7bb",
          "message": "Bump org.codehaus.mojo:exec-maven-plugin from 3.1.1 to 3.6.3 (#4552)\n\nBumps [org.codehaus.mojo:exec-maven-plugin](https://github.com/mojohaus/exec-maven-plugin) from 3.1.1 to 3.6.3.\n- [Release notes](https://github.com/mojohaus/exec-maven-plugin/releases)\n- [Commits](https://github.com/mojohaus/exec-maven-plugin/compare/3.1.1...3.6.3)\n\n---\nupdated-dependencies:\n- dependency-name: org.codehaus.mojo:exec-maven-plugin\n  dependency-version: 3.6.3\n  dependency-type: direct:development\n  update-type: version-update:semver-minor\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>\nCo-authored-by: Ivo Gaydazhiev <ivo.gaydazhiev@redis.com>",
          "timestamp": "2026-06-08T13:05:40Z",
          "url": "https://github.com/redis/jedis/commit/c98737cd3666551ad454597cf52c61b6124bd7bb"
        },
        "date": 1781056940694,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11348.696711532753,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 693524.4202538555,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 612593.9955592674,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11650.353781646321,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7708.5005842846485,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11699.77368948836,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 697237.5742095554,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 612475.2310403284,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11261.757170298655,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 53264.9561897079,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1854065.1555580874,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1387883.3181237273,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 51895.963247678046,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38091.591451443725,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1640702.2007527621,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1231536.8121993663,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37783.23139697137,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11343.18165743719,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37620.51708706362,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11341.380928864364,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17465.918350014763,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50582.43354302906,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11382.508093540475,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37299.88567092868,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.09666032298655,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.3431788903199,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.45146990518435,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.518802268024643,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 85.00400672446476,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.43868752449985,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.58094032847096,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.503648208650185,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18484.344285103114,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 202.08263363929333,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 194575.7982212472,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 212.90012984846553,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.56825634671024,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 38.6170408932869,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 79.6425934473417,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.507106792438766,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.281159361825196,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781143757586,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11265.474611554004,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 682732.0430185247,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 602102.0816064843,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 10882.67822343272,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7431.113833110711,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11080.902040563387,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 687976.5687207098,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 601446.435476448,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11002.862750480124,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54484.774153221886,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1882911.3295369498,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1412511.2415871583,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 51996.799725028606,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37403.43007340311,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1627435.5693862352,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1229221.8446948668,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36867.952820973915,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10898.424451163944,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37060.29339595517,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11038.01083019424,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17160.68575134975,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50967.28916298215,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10811.079903555938,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37758.56783847428,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.73624448693059,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.44003510706535,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 112.89898815134632,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.54932296185737,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.49471132861281,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.292756850388898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 111.07858994560357,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.534521844321457,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 19750.918479176595,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 227.80188832090025,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 193999.56572871545,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 199.46524146903388,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.09724045659978,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.02294056448104,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 83.36657332689927,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.59501426590915,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.38949764543612,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781230044466,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11387.959115698197,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 695449.1116215454,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 611231.4631587388,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11157.090062433515,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7054.831262173944,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11011.21502968411,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 694358.0230703781,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 605161.5765746234,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10976.95356759745,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54207.40927016463,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1850104.3667460072,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1403922.0128191116,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52871.56624878875,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37762.59213673068,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1615994.4708781736,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1231864.602296542,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37145.802195169126,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10997.215220546761,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37066.932384019194,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11066.510413716835,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17090.398668866306,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50102.18757966225,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11059.260055227602,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37069.18087878016,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.71968463408255,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.368063380821475,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 112.07179590054542,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.64380802184398,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.96642494159556,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.995253053196315,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.79587646606012,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.581752062744537,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 20530.712873988447,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 199.31556465276395,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 185304.8400728177,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 240.41675318154572,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.21637148512207,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.18900916081168,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.58918578472664,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 31.05229247156086,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.30209316527436,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781316174689,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11416.703972663043,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 696897.2534241237,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 607215.009895947,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11413.503936935613,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7072.185400164552,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11122.345550133889,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 697439.1004655032,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 616249.4340877304,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11271.755825397486,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55277.16003250189,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1869360.006361749,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1402874.944457938,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 53647.146488919054,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37848.113495367914,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1633491.673103418,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1232957.168935356,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37999.213935135704,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11073.079805215206,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37612.798886706776,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11558.004712832779,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17688.8599638715,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50523.78119471505,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11295.90092609924,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37661.53014197755,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 84.97513984767076,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.453940251085072,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.64172739097071,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.588978857211625,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.85560999012475,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.351692384816314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 111.03271750565648,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.574392220705898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21637.365587560307,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 227.02503163406885,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 193943.25257692306,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 200.59460427499772,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 110.15948025499267,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.15984129272245,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 81.60978677753594,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.538649779071818,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.35932684826075,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781402829314,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7962.624585523125,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 534962.7742185614,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 466661.3648278256,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7827.090996800691,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5248.777063858715,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7800.846356055704,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 535731.0597905498,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 465373.819397759,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7753.68335948212,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 47940.00152173118,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1661332.4988131183,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1190360.6789858986,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 46910.32269232693,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 31261.480022224114,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1333905.8045984597,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1008623.900903717,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 30858.87814830646,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7789.56662740952,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 30812.1886987135,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7827.005879445006,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12495.059074313987,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42044.02173536129,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7762.744749637251,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 30755.545140391834,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 110.70451673199145,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.28729069867898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 148.0276520699223,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.654061218502898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 111.98663136517796,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.288878914072523,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 154.2632300518995,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.4094444201383,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22191.40937037077,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 268.6328582685716,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 224607.84962172285,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 236.4430787828468,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 99.97881126275448,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.23759035661993,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 98.16437314300846,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.78588284409517,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.83777109989807,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781489331603,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11347.861462982033,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 688643.1687349317,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 604877.1061902593,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11440.589780874925,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7609.953691498369,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11509.876609415052,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 683149.5487229798,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 608820.8528855318,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11068.827046003675,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 53879.69437747558,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1874258.6823348366,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1395838.4883827057,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52237.601241972196,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37415.52420296146,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1614530.013079232,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1223877.5417638775,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36861.03657406751,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11015.412000174283,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37649.09378140882,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11244.784636129505,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17270.26598735182,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50963.69637335262,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11264.789869030632,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37386.13639268653,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.00439248507784,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 25.29424409192442,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 108.67270736409417,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.522379517989947,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.7921110176541,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.280238900592906,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.68628473757929,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.901448907684514,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21733.554236344808,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 244.23129912909866,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 193246.7938076923,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 235.07783604687748,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 111.34916539517435,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.10213184777892,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.9199781831032,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.81141684861953,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.178794157564425,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781575962439,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 8749.431167876382,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 573243.5793056308,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 501128.68943943875,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 8429.621217943124,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6328.126306226678,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 8524.16901354336,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 574037.1033465599,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 501923.63766879233,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 8349.385151587416,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48677.62734243309,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1699947.209863515,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1223273.2614543126,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 48301.231479962225,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 32339.80294241318,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1399194.5655738448,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1045210.1265309571,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 32003.033109821194,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 8528.851472129561,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 32375.49786853275,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 8630.301818035967,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 13781.22935344477,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 43439.99564489619,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 8520.085119572463,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31955.06423782137,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 109.51844991248588,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.21751904403742,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 175.27186373271212,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.632254497974722,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 110.28772021461923,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.219060951599296,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 172.5586540369042,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.343278196339933,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22564.910038672162,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 273.14107439179094,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 226392.22058426967,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 235.83210694991462,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 103.03733549194047,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.26650301995254,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 94.42144717792503,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.74518767197684,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.769626949575226,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781662171768,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11216.519907069543,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 682825.152353388,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 603948.8691153446,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 10960.427904558424,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6847.881483734632,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11000.698420332967,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 683646.934246301,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 607637.6513072208,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10698.252312701139,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 53813.53752934205,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1855623.8069606058,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1392813.861167639,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52400.26834648636,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 38598.460392206,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1640694.174276735,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1209872.4883895877,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37425.31247918443,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10866.939151595847,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37074.00315304789,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 10927.656400416308,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 16427.525726336546,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 49722.516302621596,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10658.274609875778,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36534.696541741854,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 86.1391723049023,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.43899325711128,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.59974734222138,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.515403432424879,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.74273259864079,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.36299763937195,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.6972955857801,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.654854411957945,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 19879.383553651664,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 217.61005042955185,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 194465.84638125467,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 211.97846650978082,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.8306093479321,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.10458321093982,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 83.18732573996753,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.42895447578162,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.33019846126774,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781748571200,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11143.622181248455,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 662935.7750983502,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 593637.428111243,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11053.497764270684,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6898.409300099645,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11146.053703085281,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 678553.6911895579,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 599801.0279815228,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10847.682470632475,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 54062.8210952657,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1848836.4015404438,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1409419.9217687012,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 52083.51400696505,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 53522.18984501013,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1642844.7702136412,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1222980.6513461343,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36731.00751245032,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10938.730366015992,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 36856.00833500105,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 10970.179133594007,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 16917.067285292735,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50905.088952221835,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10937.545960265448,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37299.08089183604,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.51059760203125,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.56319076989949,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 133.33152591878968,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.564743876846597,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 85.17351758184323,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.363617001039575,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 108.86527274277917,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.54221558955454,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18366.506080623883,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 246.50819931006518,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 198246.6547230704,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 197.94649115631591,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.62098091222433,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 38.62334231273597,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.96871288372093,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.44321213517646,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 37.03482114317713,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781835358493,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11532.522116930337,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 699256.5022311441,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 612399.1086807473,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 11424.00747844022,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7563.743839840592,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11248.351717611426,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 686470.7998645925,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 609690.4229713873,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 11160.474906621534,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55607.93408164418,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1837185.848210747,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1396584.6753901336,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 50076.97327483755,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37844.17257439328,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1631269.9340972784,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1228334.0738708738,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37828.081497656145,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11043.607546512209,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 36977.911848553325,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11287.041492052973,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17299.488248925838,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50857.75234634664,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 11267.368884928715,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37993.81911547624,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 85.03831421117127,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 25.318906266477082,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 110.91225457365879,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.720718011885072,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 82.31674238019018,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.30341868440921,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 110.69085335125749,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.501695346578137,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18693.529290284503,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 224.62520200112053,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 182511.18781818182,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 222.90256883799844,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.41425129987155,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.06637554342477,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 82.92285608987247,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.395674694133856,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.1585342854563,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1781920883068,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7979.187992909079,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 541187.7861449227,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 477828.91538565664,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 8038.875766495688,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5933.604990139102,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7900.390763136592,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 540954.2719859176,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 475429.36397855805,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7808.742654760665,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 49045.56683532499,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1670783.8150454448,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1207764.8418684222,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 49117.00217696326,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 32726.601490667286,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1373458.7610091944,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1031504.7918767547,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31791.277907540934,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7860.735345727339,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 31750.35887134692,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7914.361591097768,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12399.464821396703,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42483.39787338981,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7873.754491665159,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31611.159711732624,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 117.27442126086348,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.35752388050694,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 147.951218120342,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.724273461856793,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 108.8251522241801,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.246218107000118,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 146.04313309548016,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.004086505332392,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21702.267461709253,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 247.9262119486841,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 232420.30382624967,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 254.26916582191635,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 99.6669197010828,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.296506738447,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 99.50242085631182,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.75763694250039,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 47.042949403449185,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1782007733825,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 8043.807997474878,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 536170.0938412871,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 475450.4014034695,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7874.392689537209,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5378.026702753278,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7919.711454045842,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 540062.9977595607,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 480088.98898807337,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7945.2350330146,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48682.65464411597,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1660280.6839888166,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1217934.6164216339,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 48241.75266752978,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 33467.73695230745,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1355385.5504384274,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1048215.8927666426,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31762.079537841095,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 8002.689670657965,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 31831.615606333617,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 8044.516598085514,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12676.73276419819,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42941.60570056653,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7925.858829585671,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31873.35074003818,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 109.74067791283498,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.24213132992502,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 145.8823418433422,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.482374374422658,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 117.32845992846963,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.71687423021219,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 147.94949599172733,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.353954566473607,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21766.630883949212,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 250.0205932688682,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 224448.62398726592,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 248.66096978289391,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 99.38355151390992,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 35.9812447081713,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 97.8529480439031,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.59425545897017,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.71396365245752,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
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
          "id": "60b6eaa041aac701f5a5c52a410eafc4a9d81c3c",
          "message": "Tighten Connection.initializeFromClientConfig() visibility to package-private (#4548)\n\n* Fix double initialization of pooled Connection in TrackingConnectionPool\n\nConnection.Builder.build() already initializes the connection, so\nFailFastConnectionFactory.makeObject() was running initializeFromClientConfig()\na second time. This re-registered PUBSUB_CONSUMER and re-ran the HELLO/CLIENT\nhandshake on every MultiDbClient database connection.\nSplit Builder.build() into buildUninitialized() + init so the existing\nfactoryTrackedObjects set can wrap the (now sole) init — preserving the\nability for forceDisconnect() to interrupt a connection mid-handshake.\n\n* format\n\n* Tighten Connection.initializeFromClientConfig() to package-private\n\nFollow-up to #4547: no out-of-package caller remains.\n\n* test: guard pooled-connection double-init via HELLO count instead of mock-construction",
          "timestamp": "2026-06-10T12:03:45Z",
          "url": "https://github.com/redis/jedis/commit/60b6eaa041aac701f5a5c52a410eafc4a9d81c3c"
        },
        "date": 1782094147984,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7945.698292996113,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 540776.4459295729,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 474256.6704397473,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7935.617161131851,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5317.276447027839,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7870.287110415103,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 540553.8472117212,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 466047.4070932587,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7755.556857313629,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 48715.603897946115,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1652496.666645534,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1211023.2054909389,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47602.5858784179,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 31864.32618119336,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1361560.4993260428,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1022558.2898859063,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 31972.399349772495,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7890.018069553915,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 31849.88349446971,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7891.309319876324,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12410.547005872435,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 43177.87850513926,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7887.259922258391,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31953.826591604524,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 116.0157683876381,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.328818951949792,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 175.5988318541247,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.031754266256968,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 109.45914663923354,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.240733409373497,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 145.67195652575856,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.058248791477698,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 24037.095426826414,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 244.9390054642151,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 222456.49616019538,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 238.32492792500503,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 101.21657768146734,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.49113380381848,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 97.9766137527869,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.668685840588616,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.827830702947594,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "c813a43df9949367929842c4e06ebb497c1dc812",
          "message": "Bump org.sonatype.central:central-publishing-maven-plugin from 0.10.0 to 0.11.0 (#4567)\n\nBump org.sonatype.central:central-publishing-maven-plugin\n\nBumps [org.sonatype.central:central-publishing-maven-plugin](https://github.com/sonatype/central-publishing-maven-plugin) from 0.10.0 to 0.11.0.\n- [Commits](https://github.com/sonatype/central-publishing-maven-plugin/commits)\n\n---\nupdated-dependencies:\n- dependency-name: org.sonatype.central:central-publishing-maven-plugin\n  dependency-version: 0.11.0\n  dependency-type: direct:development\n  update-type: version-update:semver-minor\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-06-22T07:34:26Z",
          "url": "https://github.com/redis/jedis/commit/c813a43df9949367929842c4e06ebb497c1dc812"
        },
        "date": 1782179917156,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 13624.56180229313,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 846282.3696848381,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 754772.3653590691,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 13425.808900583634,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 12602.657724866247,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 13357.69229496557,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 857854.5971389558,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 747208.2118047305,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 13484.256647484528,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 106690.58271987244,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 2518081.5101268184,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1868275.7363058068,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 94625.16600648643,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 72559.07547671141,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 2401311.052810467,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1782231.7277741276,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 71471.01632158764,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 13318.777804081674,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 71902.49323871013,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 13559.57010324567,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 21874.843731671688,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 95067.22176038795,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 13396.013617663975,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 72851.51775324329,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 68.84335962261534,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 14.245305561327104,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 89.41861486236672,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 10.746188481741083,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 69.86025849436093,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 14.135574524689067,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 93.17451357054894,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 10.647205367942325,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 17829.886872806255,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 162.43068415072383,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 154559.25528187287,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 161.1924751113589,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 72.78735186890006,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 30.39897263545162,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 63.67720447743541,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 18.704340351701394,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 25.547392162888244,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "dependabot[bot]",
            "username": "dependabot[bot]",
            "email": "49699333+dependabot[bot]@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "c813a43df9949367929842c4e06ebb497c1dc812",
          "message": "Bump org.sonatype.central:central-publishing-maven-plugin from 0.10.0 to 0.11.0 (#4567)\n\nBump org.sonatype.central:central-publishing-maven-plugin\n\nBumps [org.sonatype.central:central-publishing-maven-plugin](https://github.com/sonatype/central-publishing-maven-plugin) from 0.10.0 to 0.11.0.\n- [Commits](https://github.com/sonatype/central-publishing-maven-plugin/commits)\n\n---\nupdated-dependencies:\n- dependency-name: org.sonatype.central:central-publishing-maven-plugin\n  dependency-version: 0.11.0\n  dependency-type: direct:development\n  update-type: version-update:semver-minor\n...\n\nSigned-off-by: dependabot[bot] <support@github.com>\nCo-authored-by: dependabot[bot] <49699333+dependabot[bot]@users.noreply.github.com>",
          "timestamp": "2026-06-22T07:34:26Z",
          "url": "https://github.com/redis/jedis/commit/c813a43df9949367929842c4e06ebb497c1dc812"
        },
        "date": 1782266257948,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11167.917126684019,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 682949.85743295,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 599145.9594125411,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 10993.224533250255,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 6994.978645676834,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 10958.963864481295,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 692430.4837547054,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 597855.4546681981,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10818.772200134437,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 50387.147869820554,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1860111.2736481752,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1394961.2661862024,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 51925.998071979186,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37516.71890967477,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1606708.301120958,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1219153.6282547605,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 36793.46401662704,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 10887.833746501232,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 37296.18507264844,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 10936.673434112823,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 16727.70178368489,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50170.52519105699,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10929.651534787079,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 37138.22884431716,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 82.12378524801628,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.360186492307875,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 108.90441098313715,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.975582712069237,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 84.51010554315647,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 21.38356881067898,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 111.34387060948465,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.81306790461521,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 18471.824416112853,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 243.48829462635626,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 200950.78229622962,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 235.1512487380327,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 109.31146563774317,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 38.67400914388678,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 81.88392132483443,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.649097385644865,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 37.01831469559008,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "c72f02cf82c8f8f548690dafe6de13c951b9d06a",
          "message": "Do not assert on internal field of XINFO command (#4571)",
          "timestamp": "2026-06-24T15:28:30Z",
          "url": "https://github.com/redis/jedis/commit/c72f02cf82c8f8f548690dafe6de13c951b9d06a"
        },
        "date": 1782352822793,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 7848.354480752026,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 524760.843560316,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 460372.9834765842,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 7760.57867393277,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 5765.011980192348,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 7759.565257417065,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 522366.7575154424,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 460443.7576527485,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 7805.8039523948855,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 47460.11850895782,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1661331.5933913437,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1188541.6978230956,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 47312.6166300552,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 32209.592859533594,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1350160.1286269545,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1012114.6609549541,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 30980.753825192212,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 7789.907361896536,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 30794.01919677316,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 7912.840376009155,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 12210.24703433685,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 42043.27036989236,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 7840.497751623424,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 31070.230762981733,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 109.37533056938739,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 29.29815688661962,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 149.9192381346345,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.349920501960062,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 110.68259087800183,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 29.274273834409506,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 153.13795624279672,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.349142859491181,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 21888.487397920348,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 267.0435393727178,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 220145.25171834687,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 242.54566804810605,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 99.41069198872316,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 36.456301592157345,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 97.87369045428514,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 42.626247535276775,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 46.882430904071654,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Igor Malinovskiy",
            "username": "uglide",
            "email": "u.glide@gmail.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "f46de703226973ecb5340e0e84de022837d70f5c",
          "message": "Verify that RADIX_TREE_KEYS and RADIX_TREE_NODES were parsed (#4573)",
          "timestamp": "2026-06-25T10:46:49Z",
          "url": "https://github.com/redis/jedis/commit/f46de703226973ecb5340e0e84de022837d70f5c"
        },
        "date": 1782439344458,
        "tool": "jmh",
        "benches": [
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.get",
            "value": 11193.522069369832,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedGet",
            "value": 690549.2210593374,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.pipelinedSet",
            "value": 598902.1821547855,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.jedis.GetSetBenchmark.set",
            "value": 10928.037705519484,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.pubsub.PubSubPushBenchmark.publishAndReceive",
            "value": 7436.791799390873,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.get",
            "value": 11120.204578882875,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedGet",
            "value": 687313.9908863199,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.pipelinedSet",
            "value": 599810.3672076198,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads1.set",
            "value": 10980.226664022197,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.get",
            "value": 55050.26936977878,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedGet",
            "value": 1882657.634432836,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.pipelinedSet",
            "value": 1384445.057971916,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads64.set",
            "value": 53448.502181872806,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 64"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.get",
            "value": 37800.84495283866,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedGet",
            "value": 1620833.814273654,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.pipelinedSet",
            "value": 1219958.776994992,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.redisclient.GetSetBenchmark.Threads8.set",
            "value": 37181.15880099833,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 1\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT1.workload",
            "value": 11063.235086249804,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisPoolT8.workload",
            "value": 36860.631943653854,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.JedisT1.workload",
            "value": 11222.940720833976,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT1.workload",
            "value": 17363.69315174357,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientCSCT8.workload",
            "value": 50698.70105268001,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT1.workload",
            "value": 10962.056074600147,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.workload.GetSetMixedR90W10Benchmark.RedisClientT8.workload",
            "value": 36881.10414919782,
            "unit": "ops/s",
            "extra": "iterations: 5\nforks: 2\nthreads: 8"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadArray",
            "value": 83.65697843072778,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadBulkString",
            "value": 21.40296641302296,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadMultiBulkResponse",
            "value": 115.82281816365287,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.cacheAwareReadSimpleString",
            "value": 14.50411124701375,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readArray",
            "value": 85.29842226529667,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readBulkString",
            "value": 25.221688385688314,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readMultiBulkResponse",
            "value": 108.99917516081068,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readSimpleString",
            "value": 14.604288669206463,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith100PushMessages",
            "value": 22400.36290884859,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadBenchmark.readWith1PushMessage",
            "value": 217.1845589277006,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1000Pending",
            "value": 195273.8461095674,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.ReadPushesBenchmark.drain1Pending",
            "value": 201.6464085934868,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.protocol.SendCommandBenchmark.measureSendCommand",
            "value": 108.17979807642948,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotBytes",
            "value": 39.10911710632043,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.CRC16Benchmark.getSlotString",
            "value": 81.66131725180882,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.decodeBytesToString",
            "value": 30.37772877565672,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          },
          {
            "name": "redis.clients.jedis.benchmark.util.SafeEncoderBenchmark.encodeStringToBytes",
            "value": 34.14347144255662,
            "unit": "ns/op",
            "extra": "iterations: 5\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}