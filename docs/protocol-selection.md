# Protocol Selection (RESP2 / RESP3)

Jedis supports the RESP2 and RESP3 protocols. The protocol of a connection is chosen during the
connection handshake by two `JedisClientConfig` settings: `protocol` and `autoNegotiateProtocol`.

## Settings

| `protocol` | `autoNegotiateProtocol` | How to set | Semantics | Description |
|---|---|---|---|---|
| `RESP2` | ignored | `protocol(RESP2)` or `resp2()` | Strict protocol | Uses RESP2. The `HELLO` command is required to enforce it. |
| `RESP3` | ignored | `protocol(RESP3)` or `resp3()` | Strict protocol | Uses RESP3. The `HELLO` command is required to enforce it. |
| not set | `true` (default) | nothing, or `autoNegotiateProtocol(true)` | Negotiated protocol | Tries RESP3, then RESP2. If the protocol cannot be enforced, falls back to the server default (assumes RESP2). |
| not set | `false` | `autoNegotiateProtocol(false)` or `serverDefaultProtocol()` | Server default protocol | The protocol is not enforced and the client assumes RESP2. |

- `autoNegotiateProtocol` is used only when no protocol is set.
- A strictly selected protocol that the server cannot provide fails the connection with
  `JedisProtocolNotSupportedException`.
- Negotiation falls back to RESP2 only when the server does not support RESP3 or does not know
  `HELLO`. Any other error, such as a wrong password, fails the connection.

The table describes `RedisClient`, `RedisClusterClient`, `RedisSentinelClient` and `MultiDbClient`.
For `Jedis` and `JedisPool` see [Legacy clients](#legacy-clients).

## Why strict selection needs `HELLO`

[`HELLO`](https://redis.io/docs/latest/commands/hello/) is the only way for a client to select a
protocol and have the server confirm it. A connection that sends no `HELLO` keeps the protocol the
server starts with. Today that is RESP2, but the
[protocol specification](https://redis.io/docs/latest/develop/reference/protocol-spec/#resp-versions)
notes that the default may change in a future version.

For that reason `protocol(...)` is strict, and connecting without `HELLO` is a separate, explicit
choice: `serverDefaultProtocol()`.

## Examples

Default: negotiate RESP3 and fall back to RESP2.

```java
RedisClient client = RedisClient.builder()
    .hostAndPort("localhost", 6379)
    .build();
```

Select RESP2 strictly, for example to keep RESP2 reply shapes. The server must support `HELLO`.

```java
RedisClient client = RedisClient.builder()
    .hostAndPort("localhost", 6379)
    .clientConfig(DefaultJedisClientConfig.builder().resp2().build())
    .build();
```

Connect to a server without `HELLO`. No `HELLO` is sent and RESP2 is assumed.

```java
RedisClient client = RedisClient.builder()
    .hostAndPort("localhost", 6379)
    .clientConfig(DefaultJedisClientConfig.builder().serverDefaultProtocol().build())
    .build();
```

## Servers without `HELLO`

`HELLO` was added in Redis 6.0. Older servers, and servers or proxies that do not provide the
command, answer it with an unknown-command error. On such a server:

- The default configuration connects: negotiation falls back to RESP2.
- `serverDefaultProtocol()` connects without sending `HELLO`.
- Strict protocol selection fails: `protocol(RESP2)` and `protocol(RESP3)` cannot be enforced
  without `HELLO`.

## Features that require RESP3

- **Client-side caching.** A client builder rejects a cache combined with a configuration that
  cannot reach RESP3: `protocol(RESP2)` or `autoNegotiateProtocol(false)`.
- **[Smart client handoffs](smart-client-handoffs.md).** Maintenance notifications arrive as RESP3
  push messages.

## Legacy clients

`Jedis` and `JedisPool` do not negotiate the protocol:

- With no protocol set they send no `HELLO` and use RESP2.
- A `JedisClientConfig` with no protocol set and `autoNegotiateProtocol` left at `true` is treated
  as `false`, and a warning is logged. Use `serverDefaultProtocol()` to avoid the warning.
- A strictly selected protocol uses `HELLO`, as in the table above.
