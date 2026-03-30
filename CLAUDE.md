# FlagZen -- Project Instructions

## Development Paradigm

**OOP (Java)**. This is a Java 17+ library using object-oriented programming with dependency inversion (ports-and-adapters). Zero runtime reflection in the core module. Compile-time annotation processing for code generation.

## Project Structure

Gradle monorepo with submodules:

- `flagzen-core` -- annotations, annotation processor, proxy generation, FeatureDispatcher, FlagProvider SPI
- `flagzen-test` -- JUnit 5 extension, @PinFlag, @FlagSource, TestFlagContext
- `flagzen-spring` -- Spring Boot auto-configuration
- `flagzen-env` -- Environment variable FlagProvider
- Provider modules: `flagzen-launchdarkly`, `flagzen-togglz`, `flagzen-openfeature`
- Reactive modules: `flagzen-reactor`, `flagzen-mutiny`

## Conventions

- Group ID: `com.flagzen`
- Package root: `com.flagzen`
- Generated proxies: `{Feature}_FlagZenProxy` in same package as `@Feature` interface
- SPI registration: `META-INF/services/`
- Java 17+ required
- No runtime reflection in flagzen-core
- All public API types must have Javadoc

## Markdown Style

This project uses markdownlint (config in `.markdownlint-cli2.jsonc`). When writing or editing `.md` files:

- Wrap Java generic types in backticks: `Predicate<String>`, `Map<String, Object>`, `Class<?>` — bare angle brackets are parsed as HTML (MD033)
- Check `.markdownlint-cli2.jsonc` for disabled rules before "fixing" something that's intentionally allowed
- Tables must have consistent column alignment

## Release Discipline

When preparing a release:

- Update `CHANGELOG.md` with a summary of changes since the last release
- Bump `flagzenVersion` in `gradle.properties` (remove `-SNAPSHOT`)
- After release, bump to next `-SNAPSHOT` version

## Commit Discipline

When creating a commit, always check `git status` for the entire repo and include all related changes — not just the files you directly edited. In particular, watch for:

- `.nwave/des/logs/` (DES audit logs)
- `docs/feature/*/deliver/` (execution logs, progress files)
- `docs/progress.md` (if milestone status changed)
- Any other files modified as side effects (linter fixes, generated files, config changes)

Do not leave tracked files with pending changes out of a commit. If unrelated changes exist, commit them separately with an appropriate message rather than leaving them dangling.

In commit messages, wrap any `@`-prefixed word in backticks (e.g., `` `@Feature` ``) so GitHub does not interpret it as a user mention.

## Architecture

See `docs/feature/flagzen/design/architecture-design.md` for full architecture.
See `docs/adrs/` for architectural decision records.

## Key Design Decisions

1. One proxy class generated per @Feature interface (not a registry)
2. FlagProvider contract: `Optional<String> getString(String key)` (string-only for Release 1)
3. FeatureDispatcher is an interface with default factory method; concrete implementation is internal
4. Generated proxies: public class, package-private constructor
5. Zero runtime reflection in core -- all dispatch via compile-time generated code

## Mutation Testing Strategy

This project uses **per-feature** mutation testing. PITest runs in CI after the build job succeeds, scoped to `com.flagzen.*` classes in flagzen-core. Kill rate gate: >= 80%. Current baseline: 86% kill rate.

## Progress Tracking

See `docs/progress.md` for milestone-based progress against the project brief. Each milestone has an nWave feature-id (e.g., `flagzen-eval-context`) for use with `/nw-deliver`, `/nw-design`, etc. Update `docs/progress.md` when:

- A milestone item is completed (check the box)
- A milestone status changes (NOT STARTED -> IN PROGRESS -> DONE)
- New scope is added to the project brief
