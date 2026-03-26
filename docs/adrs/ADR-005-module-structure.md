# ADR-005: Module Structure

## Status

Accepted

## Context

FlagZen is a library with multiple concerns: core annotations/processor, testing support, DI integration, flag provider adapters, and reactive context propagation. The module structure determines:

1. What dependencies consumers pay for
2. How independently concerns can evolve
3. How community contributors can own specific modules
4. What the consumer's Gradle/Maven dependency declaration looks like

### Quality Attributes

- **Maintainability**: each module has a single, clear responsibility
- **Portability**: consumers only pull dependencies they need
- **Extensibility**: new providers/integrations are new modules, not core changes

## Decision

Gradle monorepo with the following submodules, each published as a separate Maven artifact under `com.flagzen`:

|        Module        |           Artifact ID            |                         Responsibility                          |
| -------------------- | -------------------------------- | --------------------------------------------------------------- |
| flagzen-core         | com.flagzen:flagzen-core         | Annotations, processor, proxy gen, dispatcher, FlagProvider SPI |
| flagzen-test         | com.flagzen:flagzen-test         | JUnit 5 extension, @PinFlag, @FlagSource, TestFlagContext       |
| flagzen-env          | com.flagzen:flagzen-env          | Environment variable FlagProvider                               |
| flagzen-spring       | com.flagzen:flagzen-spring       | Spring Boot auto-configuration                                  |
| flagzen-reactor      | com.flagzen:flagzen-reactor      | Reactor context propagation                                     |
| flagzen-mutiny       | com.flagzen:flagzen-mutiny       | Mutiny context propagation                                      |
| flagzen-launchdarkly | com.flagzen:flagzen-launchdarkly | LaunchDarkly adapter                                            |
| flagzen-togglz       | com.flagzen:flagzen-togglz       | Togglz adapter                                                  |
| flagzen-openfeature  | com.flagzen:flagzen-openfeature  | OpenFeature adapter                                             |

All modules share the same version (aligned release). Version managed via root `gradle.properties`.

### Release 1 Modules (MVP)

Only these modules are built and published in Release 1:

- flagzen-core
- flagzen-test

### Release 2 Modules

- flagzen-env
- flagzen-spring

### Release 3+ Modules

- All provider adapters and reactive modules

## Alternatives Considered

### Alternative 1: Single Module (Monolith JAR)

Everything in one `flagzen` artifact with optional dependencies.

- **Pro**: Simplest build configuration
- **Pro**: One dependency declaration for consumers
- **Con**: Consumers pull Spring Boot, LaunchDarkly SDK, Reactor, etc. as optional/transitive deps even if unused
- **Con**: Version conflicts between Spring Boot 2 vs 3, Reactor versions, etc.
- **Con**: Cannot evolve provider adapters independently
- **Con**: Contributor ownership is unclear -- every change touches the same module

**Rejected**: Forces unnecessary dependencies on consumers. Prevents independent evolution of concerns. Classic "monolith JAR" anti-pattern for libraries.

### Alternative 2: Two Modules (Core + Extensions)

`flagzen-core` and `flagzen-extensions` (all integrations in one module).

- **Pro**: Simpler than full decomposition
- **Pro**: Two dependency declarations maximum
- **Con**: Extensions module has conflicting dependencies (Spring + CDI + Reactor + Mutiny)
- **Con**: Adding a new provider requires releasing the entire extensions module
- **Con**: Consumers still pull unwanted transitive dependencies from the extensions module

**Rejected**: The extensions module becomes a dependency magnet. Spring users pull Quarkus dependencies and vice versa. Defeats the purpose of modular design.

### Alternative 3: Annotation Processor as Separate Module

Split flagzen-core into `flagzen-annotations` (runtime annotations) and `flagzen-processor` (compile-time processor).

- **Pro**: Clean separation of compile-time and runtime code
- **Pro**: Consumers can declare annotations as `implementation` and processor as `annotationProcessor`
- **Con**: Adds complexity -- two dependencies for the basic use case
- **Con**: The processor is already compile-time-only via standard annotation processor discovery
- **Con**: Most Java libraries ship annotations + processor in one artifact (MapStruct, Lombok, Dagger)

**Rejected**: Industry convention is to ship annotations and processor together. The processor is already isolated to compile time by the Java annotation processing mechanism. Splitting adds consumer friction for no practical benefit.

## Consequences

### Positive

- Consumers declare exactly the modules they need -- no unnecessary transitive dependencies
- Each module can evolve independently (within version alignment)
- Community contributors can own specific modules (e.g., flagzen-togglz)
- Clear responsibility per module -- easy to understand and maintain
- Provider SDK version conflicts isolated to their respective modules

### Negative

- More Gradle build configuration (mitigated by convention plugins)
- Version alignment requires coordinated releases (mitigated by Gradle version catalog)
- Consumers may need to declare 2-3 dependencies (core + test + spring) -- minor friction
- BOM/platform dependency recommended for consumers to avoid version mismatches
