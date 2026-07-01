# Integration Testing Infrastructure

This document describes how Jedis integration tests are wired up: the Redis test
environment, how it is started, how tests discover their servers, and how this
maps to CI.

## Tips & tricks

Tests do not start Redis. They connect to servers that are already running. The
servers are defined by `src/test/resources/env/docker-compose.yml`, started via
`make start` (`Makefile`), and described to the tests by
`src/test/resources/endpoints.json`. A test selects a server **by name** through
`Endpoints.getRedisEndpoint("<name>")`
(`src/test/java/redis/clients/jedis/Endpoints.java`).

- **Run integration tests locally**: `make start version=8.6 && mvn clean verify && make stop`.
- **Run one test fast**: `make start version=8.6` then `mvn -Dtest=YourIT verify`
  (don't forget `make stop`).
- **Add a new server**: add a service to `src/test/resources/env/docker-compose.yml`
  (set `REDIS_CLUSTER`/`TLS_ENABLED`/`TLS_CLIENT_CNS`/ports/volumes), then add a
  matching named entry to `src/test/resources/endpoints.json`.
- **Reference it in a test**: `Endpoints.getRedisEndpoint("<name>")`
  (`src/test/java/redis/clients/jedis/Endpoints.java`) →
  `endpoint.getClientConfigBuilder().build()`.
- **TLS in a test**: build a truststore from `endpoint.getCertificatesLocation()`
  via `TlsUtil` (`src/test/java/redis/clients/jedis/util/TlsUtil.java`); certs
  come from `<TEST_WORK_FOLDER>/<env>/work/tls/`.
- **A test got skipped, not run**: its endpoint name is missing from the active
  endpoints file (`src/test/resources/endpoints.json` for `oss-docker`,
  `src/test/resources/endpoints_source.json` for `oss-source`) — you're probably
  on the `oss-source` (local) provider.
- **Reproducing CI**: the test build pins **Java 8**.

---

## 1. The test environment is provided by a shared container

All Redis servers used in tests run inside the **`redislabs/client-libs-test`**
Docker image, published publicly on
[Docker Hub](https://hub.docker.com/r/redislabs/client-libs-test). The same image
is used across several Redis client libraries (Jedis, Lettuce, redis-py,
node-redis, go-redis, …).

The image wraps a chosen Redis version and bootstraps a **standalone**,
**replication + sentinel**, or **cluster** topology — optionally with
**TLS / mTLS** and modules — entirely from environment variables. Tags follow the
Redis version (e.g. `redislabs/client-libs-test:8.6`). It is **not** for
production use.

You normally never run it by hand — `make start` brings up the whole topology via
Docker Compose (see §4). But the image is self-contained, so you can launch a
single instance directly to experiment:

```bash
# standalone on :6379
docker run --rm -p 6379:6379 redislabs/client-libs-test:8.6
# 3-node cluster on :7000-7002
docker run --rm -p 7000-7002:7000-7002 \
  -e REDIS_CLUSTER=yes -e NODES=3 -e PORT=7000 redislabs/client-libs-test:8.6
```

### 1.1 Container behaviour

| Concern          | How it works                                                                                                                                                                                                                                   |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Base image       | `redis:<tag>` (OSS) or `redis/redis-stack:<tag>` (with modules), selectable at build time via `BASE_IMAGE` / `BASE_IMAGE_TAG`.                                                                                                                 |
| Topology         | `REDIS_CLUSTER=yes` → cluster (auto-bootstrapped with `redis-cli --cluster create`); otherwise standalone/replicated. `NODES=N` sets node count (cluster default 3, standalone 1). `REPLICAS=N` sets replicas per cluster master.              |
| Ports            | Non-TLS nodes: `PORT, PORT+1, …` (default `PORT=3000`). TLS nodes: `TLS_PORT, TLS_PORT+1, …` (default `TLS_PORT=4430`).                                                                                                                        |
| Auth             | `REDIS_PASSWORD` → `requirepass`+`masterauth`. `REDIS_CLIENT_USER`/`REDIS_CLIENT_PASSWORD` are the credentials the container itself uses to create/check the cluster.                                                                          |
| Protected mode   | `PROTECTED_MODE=yes` sets Redis `protected-mode` (default `no`).                                                                                                                                                                                |
| TLS              | `TLS_ENABLED=yes` auto-generates a self-signed CA, a server cert, and client certs into `/redis/work/tls/`, and enables a TLS port per node.                                                                                                   |
| mTLS             | `TLS_CLIENT_CNS="cn1 cn2 …"` generates one client cert (+ `.p12`) per CN. `TLS_AUTH_CLIENTS_USER=CN` maps the cert CN to a Redis ACL user (Redis ≥ 8.6); `off` disables client-cert auth.                                                      |
| Modules          | When the base image ships modules (e.g. `redis/redis-stack:<tag>`), they are auto-loaded. `redislabs/client-libs-test` has matching `…-stack` style tags for module testing.                                                                    |
| Extra directives | Any trailing args become `redis-server` flags (e.g. `--maxmemory 256mb`). A few are container-managed and cannot be overridden: `--port`, `--dir`, `--logfile`, `--pidfile`, `--cluster-enabled`, `--cluster-config-file`, `--protected-mode`. |

### 1.2 Volumes & directory conventions

| Mount                              | Purpose                                                                                                                                                                                                                                                                     |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-v <host>/config:/redis/config:r` | **Input.** Pre-baked per-node config. Node dirs must be named `node-<port>[-<tlsport>]`; a sentinel node dir must be named `node-sentinel-<...>`. Each may contain `redis.conf` and (for cluster) `nodes.conf`. Optional `tls/` subdir supplies pre-generated certificates. |
| `-v <host>/work:/redis/work:rw`    | **Output.** Runtime state: `node-<i>/{redis.conf,redis.log,redis.pid,nodes.conf}` and generated `tls/` (`ca.crt`, `ca.key`, `redis.crt`, `redis.key`, `<cn>.p12`, …). **Tests read TLS certs from here.**                                                                   |

### 1.3 Generated TLS material

When `TLS_ENABLED=yes`, the container writes these files to its
`/redis/work/tls/` directory (surfaced on the host under the `work/` mount — see
§1.2 — which is where the tests read them from):

| File | What it is |
|---|---|
| `ca.crt`, `ca.key` | Self-signed test CA (signs everything else). |
| `redis.crt`, `redis.key` | Server certificate (CN `localhost`). |
| `<cn>.crt`, `<cn>.key`, `<cn>.p12` | One client cert per name in `TLS_CLIENT_CNS`, for mTLS. |

The PKCS#12 keystores (`*.p12`) use the password **`changeit`**. Certs are
generated once and reused on container restart.

### 1.4 "Endpoint" definition

An **endpoint** is a `host:port` pair for one Redis node (`127.0.0.1:6379`
non-TLS, `127.0.0.1:6390` TLS). For cluster, every node is an endpoint and the
client discovers the rest via `CLUSTER NODES`.

---

## 2. How Jedis defines its environment

Everything lives under **`src/test/resources/env/`**.

```
src/test/resources/env/
├── docker-compose.yml         # all Redis services (standalone/sentinel/cluster/tls/mtls/stack)
├── .env                       # base vars (default REDIS_VERSION, image name, work dir)
├── .env.v6.2 … .env.v8.10     # per-version overrides (just pin REDIS_VERSION)
├── redis1-2-5-8-sentinel/     # one dir per environment
│   ├── config/node-<port>/redis.conf   # mounted read-only into the container
│   └── work/                  # runtime state (TLS certs land in work/tls/)
├── standalone2-sentinel/
├── cluster-unbound/
├── ...
```

Key `.env` values:

```
REDIS_VERSION=8.6.1
CLIENT_LIBS_TEST_IMAGE=redislabs/client-libs-test
REDIS_ENV_CONF_DIR=./                       # base for the :/redis/config mounts
REDIS_ENV_WORK_DIR=/tmp/redis-env-work      # base for the :/redis/work mounts
```

Each `docker-compose.yml` service is one `client-libs-test` container, e.g.:

```yaml
redis1-2-5-8-sentinel:
  <<: *client-libs-image       # image: ${CLIENT_LIBS_TEST_IMAGE}:${REDIS_VERSION}
  environment: [REDIS_CLUSTER=no, REDIS_CLIENT_USER=deploy,
                REDIS_CLIENT_PASSWORD=verify, TLS_ENABLED=yes]
  ports: ["6379:6379", "6390:6390", "26379:26379", "36379:36379"]
  volumes:
    - ${REDIS_ENV_CONF_DIR}/redis1-2-5-8-sentinel/config:/redis/config:r
    - ${REDIS_ENV_WORK_DIR}/redis1-2-5-8-sentinel/work:/redis/work:rw
```

The compose file provides the full matrix: standalone (+TLS/+ACL), sentinel,
`cluster-unbound`, `cluster-stable` (+TLS), `standalone-mtls`, `cluster-mtls`,
a Redis-Stack service, Toxiproxy (network fault injection), and an explicitly
unavailable instance for failover tests.

---

## 3. How tests discover servers: `endpoints.json`

Tests never hardcode hosts. They look up a named entry in
**`src/test/resources/endpoints.json`**, loaded by `Endpoints.getRedisEndpoint(name)`.

### 3.1 Schema

```jsonc
{
  "standalone0-tls": {
    "endpoints":     ["rediss://localhost:6390"], // one URI (standalone) or many (cluster)
    "tls":           true,                         // required
    "username":      "default",                    // optional ACL user
    "password":      "foobared",                   // optional
    "tls_cert_path": "redis1-2-5-8-sentinel/work/tls", // optional; resolved under TEST_WORK_FOLDER
    "bdbId":         0                             // optional (Redis Enterprise only)
  }
}
```

`redis://` = plaintext, `rediss://` = TLS. Names encode the topology and mode,
e.g. `standalone0`, `standalone0-acl`, `standalone0-tls`,
`standalone0-tls-wronghost` (CN-mismatch negative test), `cluster-stable`,
`cluster-stable-tls`, `cluster-mtls`, `sentinel-standalone0`.

### 3.2 The Java side (`src/test/java/redis/clients/jedis/`)

| Class              | Role                                                                                                                                                                                                                                         |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Endpoints`        | Loads the JSON at class-init; `getRedisEndpoint(name)` returns an `EndpointConfig` (or throws `TestAbortedException` → test **skipped**, not failed, when the endpoint is absent).                                                           |
| `EndpointConfig`   | Parsed entry (Gson, `LOWER_CASE_WITH_UNDERSCORES`). Provides `getHostAndPort()`, `getHostsAndPorts()`, `getPassword()`, `isTls()`, `getCertificatesLocation()`, and `getClientConfigBuilder()` (pre-wired credentials + SSL socket factory). |
| `util/TestEnvUtil` | Chooses which endpoints file to load (see §6).                                                                                                                                                                                               |
| `util/TlsUtil`     | Builds a JCEKS truststore from the `tls_cert_path` cert dir(s) and sets `javax.net.ssl.trustStore*` system properties.                                                                                                                       |

Typical usage:

```java
@BeforeAll static void setUp() {
  endpoint = Endpoints.getRedisEndpoint("standalone0-tls");
}
// ...
try (Jedis j = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build())) { ... }
```

mTLS / TLS tests build a truststore from the cert location(s):

```java
trustStorePath = TlsUtil.createAndSaveTestTruststore(name, List.of(endpoint.getCertificatesLocation()), "changeit");
TlsUtil.setCustomTrustStore(trustStorePath, "changeit");
```

---

## 4. Running the tests (Makefile)

| Command                  | Effect                                                                                                                                |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `make start version=8.6` | `docker compose … up -d --wait` with `.env` + `.env.v8.6`. Supported: 6.2, 7.2, 7.4, 8.0, 8.2, 8.4, 8.6, 8.8, 8.10 (default **8.8**). |
| `make stop`              | `docker compose … down`.                                                                                                              |
| `make test [version=…]`  | `start` → `mvn clean verify` → `stop`.                                                                                                |
| `mvn clean verify`       | Run tests against an already-started env (use this from the IDE between `make start`/`make stop`).                                    |

Custom image instead of a version: `make start CLIENT_LIBS_TEST_IMAGE_TAG=<tag>`.

### Test selection (JUnit 5 tags + Maven, see `pom.xml`)

| Group       | Tag / naming                                                              | Runner   | Default                                |
|-------------|---------------------------------------------------------------------------|----------|----------------------------------------|
| Unit        | `@Tag("unit")`, no network                                                | Surefire | run (`skipUnitTests=false`)            |
| Integration | `*IT` (new) — legacy `*IntegrationTest` / `@Tag("integration")` still run | Failsafe | run (`skipIntegrationTests=false`)     |
| Scenario    | `@Tag("scenario")`                                                        | Failsafe | **skipped** (`skipScenarioTests=true`) |

New integration tests **must** be named `*IT` — do not use `*IntegrationTest` or
the `@Tag("integration")` marker. See §5.

Useful flags: `-DskipUnitTests=true` (CI Docker job runs integration only),
`-Dtest=<Class#method>` to narrow.

---

## 5. Test source layout & where to put tests

All test code lives under **`src/test/java/redis/clients/jedis/`**. The package
root holds the broad client/pool/connection tests; everything else is grouped by
area:

```
src/test/java/redis/clients/jedis/
├── commands/          # command coverage (integration)
│   ├── jedis/         #   against the legacy Jedis API
│   ├── unified/       #   against the UnifiedJedis surface
│   └── commandobjects/
├── search/  json/  timeseries/   # module command tests (integration)
├── tls/               # TLS / mTLS / SSL options (integration, *IT.java)
├── csc/               # client-side caching (integration)
├── failover/          # multi-db / failover (integration, @Tag("failover"))
├── authentication/    # auth + token-based auth
├── mcf/  providers/  executors/  prefix/   # connection/provider internals
├── params/  builders/  resps/              # mostly unit (arg/response objects)
├── mocked/            # pure unit, Mockito-based — no server
├── scenario/          # @Tag("scenario") — long-running, skipped by default
├── examples/          # io.redis.examples — excluded from test runs
├── benchmark/  codegen/                    # excluded from test runs
└── util/              # shared test helpers (TestEnvUtil, TlsUtil, …) — not tests
```

### Unit vs. integration: the rule

What decides whether a test is run by Surefire (unit) or Failsafe (integration)
is **how the test is marked** — its file-name suffix and/or JUnit tag — not which
folder it sits in (the runners select by suffix and tag — see §4 and `pom.xml`).

**New integration tests MUST be named `*IT`.** This is the single accepted
convention for new code. Failsafe still recognises the older `*IntegrationTest`
name and the `@Tag("integration")` marker so the many existing classes keep
running while they are migrated, but **do not use them for new tests** — name
your class `FooIT` and nothing else.

| Test needs… | Name / mark it as… | Runner |
|---|---|---|
| **No server** (pure logic, mocks) | a plain `*Test` / `*Tests` class, **untagged** | Surefire |
| **A running Redis** | **`*IT`** file name | Failsafe |
| **A long-running scenario** | `@Tag("scenario")`, placed under `scenario/` | Failsafe (off by default) |

Guidance for new tests:

- **Integration test** → name the file `FooIT.java` (the **only** accepted name
  for new integration tests — not `*IntegrationTest`, and **never**
  `@Tag("integration")`). Put it in the area package it exercises (`commands/…`,
  `search/`, `tls/`, etc.). It connects to a server resolved through
  `Endpoints.getRedisEndpoint(...)`.
- **Unit test** → add it next to related unit tests (or under `mocked/` if it is
  Mockito-based), name it `FooTest`, and leave it untagged. It must pass with no
  Redis running.
- **Never** put production-required fixtures in `examples/`, `benchmark/`, or
  `codegen/` — those packages are excluded from the test run. Shared helpers go
  in `util/` (helpers there are not collected as tests even if named `*Test`).

---

## 6. Two environments: Docker (full) vs. local-from-source (limited)

Selection is via the **`TEST_ENV_PROVIDER`** env var, read by `TestEnvUtil`:

| Provider                   | Endpoints file                  | Servers                                                                                                                                                            | Use                                        |
|----------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| `oss-docker` (**default**) | `endpoints.json`                | Full compose matrix (standalone, sentinel, cluster, TLS, mTLS, stack)                                                                                              | Full integration coverage.                 |
| `oss-source`               | `endpoints_source.json`         | 3 plain `redis-server` processes started by `make start-local`: `standalone0` (6379, ACL users), a Unix-socket instance, and one deliberately-unavailable instance | Lightweight local dev; **limited** subset. |
| `re`                       | (`REDIS_ENDPOINTS_CONFIG_PATH`) | Redis Enterprise                                                                                                                                                   | Internal.                                  |

Override the endpoints file directly with `REDIS_ENDPOINTS_CONFIG_PATH`, and the
TLS cert root with `TEST_WORK_FOLDER` (default `/tmp/redis-env-work`).

### Local-from-source flow

```bash
make system-setup    # CI only: build Redis from github.com/redis/redis + the test module (testmodule.so)
make test-local      # = start-local (TEST_ENV_PROVIDER=oss-source) → mvn verify → stop-local
```

`make start-local` runs three local `redis-server` instances and `make
mvn-test-local` forces `TEST_ENV_PROVIDER=oss-source`, so tests load
`endpoints_source.json`. Endpoints absent there (cluster, sentinel, TLS, mTLS,
stack) resolve to a `TestAbortedException` and are **skipped** — that is why the
local env runs only a subset. Full coverage requires the Docker env.

---

## 7. CI workflows (`.github/workflows/`)

| Workflow             | Environment                     | What it does                                                                                                                                                                                                                                                                          |
|----------------------|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test-on-docker.yml` | **Docker (full)**               | Matrix over Redis 7.2 → 8.10. Uses the `run-tests` composite action: `make start version=<v>` → `TEST_ENV_PROVIDER=oss-docker`, `TEST_WORK_FOLDER=$REDIS_ENV_WORK_DIR`, `mvn -B -DskipUnitTests=true clean compile verify` → `make stop`. Uploads JaCoCo + Surefire/Failsafe reports. |
| `integration.yml`    | **Local-from-source (limited)** | Java 8; `make system-setup` builds Redis from source + the test module; `make test-local` runs the `oss-source` subset.                                                                                                                                                               |

> Both clients pin **Java 8** for the test build. Use it when reproducing CI.
