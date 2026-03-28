# Contributing to FlagZen

Thank you for your interest in contributing to FlagZen. This guide covers how to set up the project, run tests, and submit changes.

## Prerequisites

- Java 17 or later
- Gradle 9+ (included via wrapper)

## Building

```bash
./gradlew build
```

This compiles all modules, runs annotation processing, executes unit tests, acceptance tests, and Spring integration tests.

## Running Tests

```bash
# All tests
./gradlew build

# Core unit tests only
./gradlew :flagzen-core:test

# Acceptance tests (Cucumber)
./gradlew :flagzen-acceptance-tests:test

# Mutation testing (PITest, flagzen-core only)
./gradlew :flagzen-core:pitest
```

The mutation testing gate requires a kill rate of 80% or higher.

## Project Structure

```text
flagzen-core/             Annotations, processor, proxy generation, SPI
flagzen-test/             JUnit 5 extension (@PinFlag, @FlagSource)
flagzen-key-mapping/      Reusable key format parsing/formatting
flagzen-env/              Environment variable FlagProvider
flagzen-spring/           Spring Boot auto-configuration
flagzen-openfeature/      OpenFeature SDK adapter
flagzen-examples/         Runnable examples (not published)
flagzen-acceptance-tests/ Cucumber BDD acceptance tests
```

## Code Conventions

- **Java 17+** — use records, sealed classes, pattern matching where appropriate
- **Zero runtime reflection** in `flagzen-core` — all dispatch is compile-time generated
- **Javadoc** on all public types and methods
- **Conventional commits** — `feat:`, `fix:`, `docs:`, `test:`, `chore:`

## Making Changes

1. Fork the repository and create a branch from `main`
2. Make your changes
3. Add or update tests — every behavior change needs a test
4. Run `./gradlew build` and ensure all tests pass
5. Run `./gradlew :flagzen-core:pitest` if you changed `flagzen-core` — kill rate must stay above 80%
6. Submit a pull request using the PR template

## Pull Request Guidelines

- Keep PRs focused — one feature or fix per PR
- Write a clear description of what changed and why
- Reference any related issues
- Ensure CI passes before requesting review

## Reporting Bugs

Use the [bug report template](https://github.com/FlagZen/FlagZen/issues/new?template=bug_report.yml) on GitHub Issues.

## Requesting Features

Use the [feature request template](https://github.com/FlagZen/FlagZen/issues/new?template=feature_request.yml) on GitHub Issues.

## Security

See [SECURITY.md](SECURITY.md) for reporting security vulnerabilities.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE.txt).
