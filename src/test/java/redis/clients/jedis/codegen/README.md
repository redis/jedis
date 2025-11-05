# Code Generators

This package contains code generation tools for the Jedis project. These are **not tests** and should not be executed as part of the test suite.

## CommandFlagsRegistryGenerator

Automatically generates and updates the static flags registry in `CommandObject.java` by retrieving command metadata from a running Redis server.

### Purpose

The `CommandObject` class uses a static registry to map Redis command names to their flags.

### How It Works

1. **Connects** to a Redis server (default: localhost:6379)
2. **Retrieves** all command metadata using the `COMMAND` command
3. **Processes** commands and subcommands, extracting their flags
4. **Groups** commands by their flag combinations
5. **Generates** a static initializer block with inline `EnumSet` creation
6. **Updates** `CommandObject.java` automatically using regex pattern matching
7. **Saves** a backup JSON file for offline use

### Prerequisites

- A running Redis server (version 7.0+ recommended for full command metadata)
- The Redis server should have all modules loaded if you want to include module commands

### When to Run

Run this generator whenever:
- Upgrading to a new Redis version
- New Redis modules are added to your server
- Command flags are modified in Redis
- You want to ensure the registry is up-to-date with your Redis server

### Fallback Mode

If the generator cannot connect to Redis, it will automatically fall back to using the backup JSON file (`redis_commands_flags.json`) if available.

### Output

The generator will:
- ✓ Connect to Redis and retrieve command metadata
- ✓ Process commands and subcommands
- ✓ Group commands by flag combinations
- ✓ Generate the complete static initializer block
- ✓ Update `src/main/java/redis/clients/jedis/CommandObject.java` in-place
- ✓ Save a backup JSON file for offline use
- ✓ Preserve original command names (with spaces, dots, hyphens)

