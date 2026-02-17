# Contributing to HyperFactions

Thank you for your interest in contributing to HyperFactions! This guide covers the development setup and contribution workflow for HyperFactions and the broader HyperSystems ecosystem.

## Getting Started

### Prerequisites

- **Java 25** (build and runtime)
- **Gradle 9.3.0+** (included via wrapper — no manual install needed)
- **Hytale Server JAR** (Early Access)
- **Git** for version control

### Repository Structure

HyperFactions lives inside the HyperSystems multi-project workspace:

```
HyperSystems/
├── HyperPerms/       # Permissions management
├── HyperHomes/       # Home teleportation
├── HyperFactions/    # This plugin
├── HyperWarp/        # Warps, spawns, TPA
├── HyperSpawns/      # Mob spawn zone control
├── servers/          # Dev and prerelease servers
├── settings.gradle   # Multi-project configuration
└── build.gradle      # Root build with shared properties
```

Each plugin has its own Git repository with independent versioning.

### Cloning

```bash
# Clone the HyperFactions repository
git clone git@github.com:HyperSystemsDev/HyperFactions.git

# If working in the full HyperSystems workspace, ensure HyperPerms is also cloned
# (HyperFactions compiles against it)
git clone git@github.com:HyperSystemsDev/HyperPerms.git
```

## Building

All builds use the root Gradle wrapper. **Never use subproject wrappers.**

```bash
# Build HyperFactions only
./gradlew :HyperFactions:shadowJar

# Build all plugins
./gradlew buildAll

# Clean and rebuild (recommended after branch switches)
./gradlew :HyperFactions:clean :HyperFactions:shadowJar --no-build-cache
```

Output JAR: `HyperFactions/build/libs/HyperFactions-<version>.jar`

### Shadow Plugin

Dependencies are relocated into the JAR to avoid conflicts:
- `com.google.gson` -> `com.hyperfactions.lib.gson`

The `jar` task has `archiveClassifier = 'plain'` to prevent it from overwriting the shadow JAR in multi-project builds.

## Deploying to Test Server

```bash
# Build all plugins and deploy to test server
./gradlew buildAndDeploy

# Deploy already-built JARs only
./gradlew deployMods
```

### Server Management

```bash
# Start the test server (uses screen)
cd servers/dev && ./start.sh

# Attach to server console
screen -r hytale-server

# Stop the server
screen -S hytale-server -X stuff "stop\n"
```

## Code Style

### Java 25 Features

- Use **records** for immutable data models
- Use **pattern matching** where it improves clarity
- Use **sealed interfaces** for closed type hierarchies

### Conventions

- **`Message.join()`** for message formatting — never `.then()` or legacy color codes
- **`@NotNull` / `@Nullable`** annotations on all public API parameters and return types
- **`ConcurrentHashMap`** for thread-safe collections
- **`CompletableFuture`** for all async storage operations
- **No raw types** — always specify generic parameters
- **Package-private by default** — only `public` what needs to be accessed externally

### Naming

- Manager classes in `com.hyperfactions.manager`
- Data records in `com.hyperfactions.data`
- Commands follow `<Action>SubCommand` naming (e.g., `ClaimSubCommand`)
- GUI pages follow `<Name>Page` naming (e.g., `FactionMainPage`)

## Commit Guidelines

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add faction treasury transfer system
fix: zone power loss flag ignored on death
docs: update permission reference for economy nodes
refactor: decompose AdminSubCommand into handler classes
chore: bump Gson to 2.11.0
```

### Rules

1. **Add specific files by name** — never use `git add -A` or `git add .`
2. **Write meaningful messages** — focus on "why" not "what"
3. **One logical change per commit** — don't mix features with refactors

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable releases only |
| `dev/phase1` | Primary development branch |
| `feat/<name>` | Feature branches (branch from `dev/phase1`) |
| `fix/<name>` | Bug fix branches |

### Workflow

1. Branch from `dev/phase1`: `git checkout -b feat/my-feature dev/phase1`
2. Make changes, commit with conventional commit messages
3. Push and open a PR against `dev/phase1`
4. After review, merge into `dev/phase1`
5. `dev/phase1` merges into `main` for releases

## Testing

```bash
# Run all tests
./gradlew testAll

# Run HyperFactions tests only
./gradlew :HyperFactions:test
```

Tests use **JUnit Jupiter 5.10.2**. Place test files in `src/test/java/`.

## Reporting Issues

- **Bug reports:** Use the [Bug Report template](https://github.com/HyperSystemsDev/HyperFactions/issues/new?template=bug_report.md) on GitHub
- **Feature requests:** Use the [Feature Request template](https://github.com/HyperSystemsDev/HyperFactions/issues/new?template=feature_request.md) on GitHub
- **Discord:** Join [our server](https://discord.gg/SNPjyfkYPc) for discussion

## License

By contributing, you agree that your contributions will be licensed under the [GPLv3](LICENSE).
