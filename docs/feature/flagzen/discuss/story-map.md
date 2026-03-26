# Story Map: FlagZen

## User: Marco Pellegrini (Senior Java Developer)

## Goal: Replace if/else feature flag conditionals with type-safe polymorphic dispatch that is testable, provider-independent, and validated at compile time

## Backbone

|                   Define Feature                    |                  Implement Variants                   |            Configure Provider             |              Resolve at Runtime               |              Test Flag Code              |            Integrate with DI             |        Manage Flag Lifecycle         |
| --------------------------------------------------- | ----------------------------------------------------- | ----------------------------------------- | --------------------------------------------- | ---------------------------------------- | ---------------------------------------- | ------------------------------------ |
| @Feature annotation on interface                    | @Variant annotation on implementation                 | FlagProvider SPI + in-memory/env provider | FeatureDispatcher.resolve() returns proxy     | @PinFlag annotation pins variant in test | Spring auto-config registers proxy beans | Usage stats, dead flag detection     |
| Variant enum (optional) for compile-time validation | @DefaultVariant for fallback                          | Provider auto-detection from classpath    | Proxy delegates to active variant             | TestFlagContext for programmatic pinning | CDI extension                            | Compile-time unused variant warnings |
| FallbackStrategy (REQUIRED/EXCEPTION/NOOP)          | Multi-feature @Variant (class implements 2+ features) | Composite provider (multiple sources)     | Evaluation context (user/tenant scoping)      | @FlagSource for file-based test config   | Quarkus extension (build-time)           | Hotspot detection                    |
|                                                     |                                                       | LaunchDarkly/Togglz/OpenFeature adapters  | Reactive context propagation (Reactor/Mutiny) | Test fixtures and helpers                |                                          | Dashboard data API                   |
|                                                     |                                                       |                                           | ScopedValue/ThreadLocal context chain         |                                          |                                          |                                      |

---

### Walking Skeleton

The thinnest end-to-end slice proving the core concept works:

|          Define Feature          |             Implement Variants              |         Configure Provider          |                 Resolve at Runtime                  |              Test Flag Code              |
| -------------------------------- | ------------------------------------------- | ----------------------------------- | --------------------------------------------------- | ---------------------------------------- |
| @Feature annotation on interface | @Variant annotation on implementation class | In-memory flag provider (hardcoded) | FeatureDispatcher.resolve() returns generated proxy | @PinFlag annotation pins variant in test |

**What this proves**: A developer can define a feature as a type, implement variants as classes, resolve the active variant through a proxy, and pin values in tests -- all without if/else, with compile-time annotation processing, and zero runtime reflection in the core path.

**What this excludes**: Variant enum validation, @DefaultVariant, FallbackStrategy, evaluation context, DI integration, file-based providers, reactive support, lifecycle management.

---

### Release 1: Core Type-Safe Dispatch (Walking Skeleton + validation)

**Outcome**: Java developer can define, implement, resolve, and test feature flags with full compile-time safety.

|     Define Feature      |       Implement Variants        |      Configure Provider       |        Resolve at Runtime         |         Test Flag Code         |
| ----------------------- | ------------------------------- | ----------------------------- | --------------------------------- | ------------------------------ |
| @Feature annotation     | @Variant annotation             | In-memory flag provider       | FeatureDispatcher.resolve()       | @PinFlag annotation            |
| Variant enum (optional) | @DefaultVariant                 | Environment variable provider | Proxy delegates to active variant | TestFlagContext (programmatic) |
| FallbackStrategy config | Compile-time variant validation | FlagProvider SPI contract     |                                   | @FlagSource (file-based)       |
|                         | Multi-feature @Variant          |                               |                                   | FlagZen JUnit 5 extension      |

**Stories**:

- US-01: Define a feature flag as a Java interface with @Feature
- US-02: Implement feature variants as classes with @Variant
- US-03: Validate variant values at compile time against optional enum
- US-04: Generate dispatch proxy at compile time via annotation processor
- US-05: Resolve active variant at runtime via FeatureDispatcher
- US-06: Configure flag source via FlagProvider SPI and in-memory provider
- US-07: Pin flag values in tests with @PinFlag annotation
- US-08: Configure test flags from properties files with @FlagSource
- US-09: Handle missing/unmatched variants with FallbackStrategy

---

### Release 2: Production Readiness (providers + Spring)

**Outcome**: Java developer can use FlagZen in a real Spring Boot production application with environment-variable-based flags.

|      Configure Provider       |               Resolve at Runtime               |       Integrate with DI        |
| ----------------------------- | ---------------------------------------------- | ------------------------------ |
| Environment variable provider | Evaluation context (basic: explicit parameter) | Spring Boot auto-configuration |
| Provider auto-detection       | Block-scoped context (ScopedValue/ThreadLocal) | @Autowired @Feature interfaces |

**Stories**:

- US-10: Resolve flags from environment variables via flagzen-env module
- US-11: Auto-configure FlagZen in Spring Boot with zero configuration
- US-12: Inject @Feature interfaces via @Autowired in Spring beans
- US-13: Pass evaluation context explicitly for user/tenant-scoped resolution
- US-14: Scope evaluation context to a code block via FlagContext.run()

---

### Release 3: Ecosystem Integration

**Outcome**: Java developer can use FlagZen with existing flag providers and reactive stacks.

|  Configure Provider  |     Resolve at Runtime     | Integrate with DI |
| -------------------- | -------------------------- | ----------------- |
| LaunchDarkly adapter | Reactive context (Reactor) | CDI extension     |
| OpenFeature adapter  | Reactive context (Mutiny)  | Quarkus extension |
| Togglz adapter       |                            |                   |
| Composite provider   |                            |                   |

---

### Release 4: Observability and Lifecycle

**Outcome**: Java developer can track flag usage, detect dead flags, and manage flag lifecycle.

|        Manage Flag Lifecycle         |
| ------------------------------------ |
| Usage statistics collection          |
| Compile-time unused variant warnings |
| Dead flag detection                  |
| Hotspot detection                    |
| Dashboard data API                   |

---

## Scope Assessment: PASS -- 14 stories across Release 1+2 (MVP), 2 bounded contexts (annotation processing, runtime dispatch), estimated 3-4 weeks

Release 1 alone: 9 stories, 1 primary bounded context (core), estimated 2 weeks. Right-sized for initial delivery cycle.

Releases 3 and 4 are explicitly deferred and will get their own feature directories when prioritized.
