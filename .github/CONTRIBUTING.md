# How to report a bug or request a feature

* GitHub issues SHOULD BE USED to report bugs and for DETAILED feature requests. Everything else belongs in the [Redis Discord Server](https://discord.gg/redis) or [Jedis Github Discussions](https://github.com/redis/jedis/discussions).

Please post general questions to the [Redis Discord Server](https://discord.gg/redis) or Github discussions. These can be closed without response when posted to Github issues.

# How to contribute by Pull Request

1. Fork Jedis repo on github ([how to fork a repo](https://docs.github.com/en/get-started/quickstart/fork-a-repo))
2. Create a topic branch (`git checkout -b my_branch`)
3. Push to your remote branch (`git push origin my_branch`)
4. Create a pull request on github ([how to create a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request))

Create a branch with a meaningful name and do not modify the master branch directly.

Please add unit tests and integration tests to validate your changes work, then ensure your changes pass all unit tests.

## Testing

Jedis runs its tests against real Redis servers started in Docker. The full
testing infrastructure — the test container, how environments are started, how
tests discover their servers, where to place unit vs. integration tests, and how
this maps to CI — is documented in **[docs/integration-testing.md](../docs/integration-testing.md)**.

When adding tests, please follow the unit vs. integration conventions described
in the guide (new integration tests are named `*IT` / `*IntegrationTest`).

## Code Convention

* Jedis uses HBase Formatter introduced by [HBASE-5961](https://issues.apache.org/jira/browse/HBASE-5961)
* You can import code style file (located to hbase-formatter.xml) to Eclipse, IntelliJ
  * line break by column count seems not working with IntelliJ
* <strike>You can run ```make format``` anytime to reformat without IDEs</strike>
* DO NOT format the source codes within `io.redis.examples` test package.
* A test class name MUST NOT end with `Example`.

### type <-> byte array conversion

* string <-> byte array : use SafeEncoder.encode()
  * Caution: use String.toBytes() directly will break GBK support!
* boolean, int, long, double -> byte array : use Protocol.toByteArray()

Thanks!
