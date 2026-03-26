# Technology Stack -- FlagZen

## Core Technologies

### Language and Runtime

| Technology | Version |       License        |                                              Rationale                                               |                                             Alternatives Considered                                             |
| ---------- | ------- | -------------------- | ---------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Java       | 17+     | GPL-2.0-CE (OpenJDK) | Target audience is modern Java. Records, sealed classes, text blocks available. Brief specifies 17+. | Java 11+ (broader adoption but loses modern features; rejected per brief), Java 21+ (too narrow adoption floor) |
| Gradle     | 8.x     | Apache 2.0           | Monorepo with submodules. Superior multi-project support vs Maven. Brief specifies Gradle.           | Maven (viable but weaker multi-module ergonomics, no Kotlin DSL), Bazel (overkill for this project size)        |

### Build and Compilation

|           Technology            |   Version    |  License   |                                                   Rationale                                                    |                                                                 Alternatives Considered                                                                 |
| ------------------------------- | ------------ | ---------- | -------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Gradle Kotlin DSL               | 8.x          | Apache 2.0 | Type-safe build scripts, IDE auto-completion, industry standard for new Java projects.                         | Groovy DSL (less type safety, legacy preference), Maven POM (XML verbosity)                                                                             |
| JavaPoet                        | 1.13.0+      | Apache 2.0 | Compile-time Java source generation. Type-safe API for building .java files. Mature (Square), well-maintained. | String templates (error-prone, no type safety for generated code), JStachio (focused on templates not code gen), Roaster (less popular, fewer features) |
| javax.annotation.processing API | JDK built-in | N/A (JDK)  | Standard annotation processing API. No external dependency.                                                    | Kotlin Symbol Processing / KSP (Kotlin-only), Google Auto (adds dependency for marginal convenience)                                                    |

### Testing

|        Technology        | Version |  License   |                                                 Rationale                                                 |                            Alternatives Considered                            |
| ------------------------ | ------- | ---------- | --------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| JUnit 5 (Jupiter)        | 5.10+   | EPL 2.0    | Industry standard Java testing framework. Extension API for @PinFlag/@FlagSource.                         | TestNG (declining adoption), JUnit 4 (legacy, inferior extension model)       |
| compile-testing (Google) | 0.21+   | Apache 2.0 | Testing annotation processors by compiling in-memory and asserting on diagnostics. De facto standard.     | Manual javac invocation (brittle), Takari compile-testing (less maintained)   |
| ArchUnit                 | 1.2+    | Apache 2.0 | Enforce architectural rules in tests (no-reflection constraint, package structure, dependency direction). | Manually reviewing imports (not automated), dependency-cruiser (JS ecosystem) |
| AssertJ                  | 3.25+   | Apache 2.0 | Fluent assertion library. Readable test assertions.                                                       | Hamcrest (less fluent API), JUnit assertions (limited expressiveness)         |

### Quality and Analysis

| Technology | Version |  License   |                             Rationale                             |                                  Alternatives Considered                                   |
| ---------- | ------- | ---------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| JaCoCo     | 0.8.11+ | EPL 2.0    | Code coverage analysis. >90% target for core module.              | OpenClover (heavier, less common), IntelliJ built-in (not CI-friendly)                     |
| Checkstyle | 10.x    | LGPL 2.1   | Code style enforcement. Ensures generated proxy code quality.     | PMD (complementary, not replacement), SpotBugs (bug detection, different focus)            |
| SpotBugs   | 4.8+    | LGPL 2.1   | Static bug detection. Catches null safety and concurrency issues. | Error Prone (Google -- viable alternative, compile-time), SonarQube (overkill for OSS lib) |
| Spotless   | 6.x     | Apache 2.0 | Code formatting enforcement (google-java-format).                 | Palantir format (less common), IDE-only formatting (not reproducible)                      |

### Documentation

| Technology |   Version    | License |                               Rationale                               |                      Alternatives Considered                       |
| ---------- | ------------ | ------- | --------------------------------------------------------------------- | ------------------------------------------------------------------ |
| Javadoc    | JDK built-in | N/A     | Standard Java API documentation. Every public type must have Javadoc. | Dokka (Kotlin-focused), AsciiDoc (not standard for Java libraries) |

## Extension Module Dependencies

These are dependencies of extension modules only -- they are NOT transitive to consumers unless the consumer explicitly adds the extension module.

### flagzen-spring

|        Dependency         | Version |  License   |
| ------------------------- | ------- | ---------- |
| spring-boot-autoconfigure | 3.2+    | Apache 2.0 |
| spring-context            | 6.1+    | Apache 2.0 |

### flagzen-test

|    Dependency     |  Version  |  License   |
| ----------------- | --------- | ---------- |
| junit-jupiter-api | 5.10+     | EPL 2.0    |
| flagzen-core      | (project) | Apache 2.0 |

### flagzen-env

|  Dependency  |  Version  |  License   |
| ------------ | --------- | ---------- |
| flagzen-core | (project) | Apache 2.0 |

### flagzen-reactor

|  Dependency  | Version |  License   |
| ------------ | ------- | ---------- |
| reactor-core | 3.6+    | Apache 2.0 |

### flagzen-mutiny

|   Dependency    | Version |  License   |
| --------------- | ------- | ---------- |
| smallrye-mutiny | 2.5+    | Apache 2.0 |

### Provider Adapters

|        Module        |           External SDK           | SDK License |
| -------------------- | -------------------------------- | ----------- |
| flagzen-launchdarkly | launchdarkly-java-server-sdk 7.x | Apache 2.0  |
| flagzen-togglz       | togglz-core 4.x                  | Apache 2.0  |
| flagzen-openfeature  | dev.openfeature:sdk 1.x          | Apache 2.0  |

## FlagZen Library License

**Apache License 2.0** -- permissive, commercially friendly, compatible with all dependencies, standard for Java OSS libraries.

## Dependency Principles

1. **flagzen-core has zero external runtime dependencies.** JavaPoet is a compile-time-only dependency of the annotation processor, not transitive to consumers.
2. **Extension modules depend only on flagzen-core + their specific external library.** No cross-extension dependencies.
3. **All dependencies are OSS with permissive licenses** (Apache 2.0, EPL 2.0, LGPL 2.1).
4. **Consumers only pay for what they use.** Adding flagzen-core brings zero transitive dependencies. Adding flagzen-spring brings Spring Boot. Adding flagzen-launchdarkly brings the LD SDK.
