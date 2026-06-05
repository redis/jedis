window.BENCHMARK_DATA = {
  "lastUpdate": 1780624972131,
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
      }
    ]
  }
}