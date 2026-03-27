# FlagZen Progress Tracker

Progress against [project-brief.md](project-brief.md). Each milestone maps to an nWave feature-id for use with `/nw-deliver {feature-id}`, `/nw-design {feature-id}`, etc.

---

## M0: Core Polymorphic Dispatch — `flagzen`

**Status: DONE** | [Artifacts](feature/flagzen/)

- [x] `@Feature` annotation on interfaces
- [x] `@Variant` annotation on implementation classes
- [x] `@DefaultVariant` fallback annotation
- [x] `FallbackStrategy` enum (REQUIRED, EXCEPTION, NOOP)
- [x] Compile-time annotation processing (`FlagZenProcessor`)
- [x] Proxy generation via JavaPoet (zero runtime reflection)
- [x] Runtime proxy re-evaluates flag on every method call
- [x] `FeatureDispatcher` interface + `FlagZen` factory
- [x] `FlagProvider` SPI (`Optional<String> getString(String key)`)
- [x] `InMemoryFlagProvider` for dev/test
- [x] `FeatureMetadata` SPI for cross-package proxy discovery
- [x] `@Feature` rejected on classes (interfaces only)
- [x] `@Variant` value validated against inner `Variant` enum
- [x] Duplicate variant values rejected
- [x] REQUIRED strategy enforces complete enum coverage
- [x] `@DefaultVariant` satisfies REQUIRED for uncovered values
- [x] `@Variant` class must implement `@Feature` interface
- [x] Multi-feature variants (`@Variant(of=...)`, `@Repeatable`)
- [x] Free-form variant values accepted when no enum present
- [x] Generated proxy: descriptive `toString()`, zero reflection
- [x] Proxy follows runtime flag value changes (dynamic dispatch)
- [x] Singleton proxy per feature per dispatcher
- [x] Clear error when no `FlagProvider` configured
- [x] EXCEPTION fallback throws with known variants listed
- [x] NOOP fallback returns safe defaults (false, 0, null)
- [x] `@DefaultVariant` takes priority over fallback strategy
- [x] `@PinFlag` annotation (`@Repeatable`, pins variant per test)
- [x] `@FlagSource` loads flags from properties file
- [x] `TestFlagContext` programmatic pinning API
- [x] `FlagZenExtension` JUnit 5 extension
- [x] Feature interface parameter injection in tests
- [x] Pin isolation between parallel tests
- [x] Priority: `@PinFlag` > `@FlagSource` > provider
- [x] Gradle monorepo, CI/CD, Maven Central publishing, PITest

## M1: Evaluation Context — `flagzen-eval-context`

**Status: NOT STARTED** | Depends on: M0

- [ ] `EvaluationContext` model (targeting key + attributes)
- [ ] Explicit context parameter on `resolve()`
- [ ] Block-scoped context (`FlagContext.run(ctx, () -> ...)`)
- [ ] ScopedValue (Java 21+) with ThreadLocal fallback
- [ ] `ContextAccessor` SPI implementation
- [ ] Resolution order: explicit > reactive > scoped > default

## M2: Typed Variants and Conditional API — `flagzen-typed-variants`

**Status: NOT STARTED** | Depends on: M0

### Typed Polymorphic Dispatch

- [ ] `FeatureType` enum (STRING, INT, BOOLEAN) — or inferred from `@Variant` attribute used
- [ ] `@Feature(type = FeatureType.INT)` attribute (default STRING for backward compat)
- [ ] `@Variant(intValue = 42)` for int-typed features
- [ ] `@Variant(booleanValue = true)` for boolean-typed features
- [ ] Compile-time validation: all variants of a feature use the same type
- [ ] Compile-time validation: `@Variant` attribute matches `@Feature(type = ...)`
- [ ] Proxy dispatches on typed value (int/boolean lookup instead of string)
- [ ] `FlagProvider.getInt(String key)` and `FlagProvider.getBoolean(String key)` for typed resolution
- [ ] REQUIRED strategy works with boolean features (exactly 2 variants: true + false)

### Conditional API (Non-Polymorphic)

- [ ] `FlagProvider.getBoolean(String key)` default method
- [ ] `FlagProvider.getInt(String key)` default method
- [ ] `FlagProvider.getLong(String key)` default method
- [ ] `FlagProvider.getDouble(String key)` default method
- [ ] `FlagProvider.getString(String key)` remains the primitive (typed methods delegate + parse)

## M3: Environment Variable Provider — `flagzen-env`

**Status: NOT STARTED** | Depends on: M0

- [ ] `EnvironmentVariableFlagProvider` implementing `FlagProvider`
- [ ] Key-to-env-var mapping convention (e.g., `checkout-flow` -> `FLAGZEN_CHECKOUT_FLOW`)
- [ ] ServiceLoader registration

## M4: Spring Integration — `flagzen-spring`

**Status: NOT STARTED** | Depends on: M0

- [ ] `FlagZenAutoConfiguration` (Spring Boot auto-config)
- [ ] `FeatureFactoryBean` for `@Autowired` injection of `@Feature` proxies
- [ ] `FlagProvider` bean auto-detected from `ApplicationContext`
- [ ] `@Variant` + `@Component` classes participate in Spring DI
- [ ] Conditional annotations (`@ConditionalOnMissingBean`)

## M5: Provider Adapters — `flagzen-providers`

**Status: NOT STARTED** | Depends on: M0

- [ ] `flagzen-launchdarkly` (LaunchDarkly SDK adapter)
- [ ] `flagzen-togglz` (Togglz adapter)
- [ ] `flagzen-openfeature` (OpenFeature SDK adapter)

## M6: Reactive Context Propagation — `flagzen-reactive`

**Status: NOT STARTED** | Depends on: M1

- [ ] `flagzen-reactor` (Reactor `Context` for Spring WebFlux)
- [ ] `flagzen-mutiny` (Mutiny `Context` for Quarkus Reactive)

## M7: Hooks and Observability — `flagzen-hooks`

**Status: NOT STARTED** | Depends on: M0

- [ ] Hook SPI for dispatch events (metrics, structured logging)
- [ ] Flag usage statistics collection
- [ ] Dead flag detection
- [ ] Compile-time warnings for unused variants
- [ ] Hotspot analysis

## M8: Extended Testing Support — `flagzen-test-extras`

**Status: NOT STARTED** | Depends on: M0

- [ ] `@FlagSource` for JSON format
- [ ] `@FlagSource` for YAML format
- [ ] JUnit 4 extension
- [ ] TestNG extension
- [ ] Test fixtures / helpers

## M9: Additional DI Frameworks — `flagzen-cdi`

**Status: NOT STARTED** | Depends on: M4

- [ ] CDI integration
- [ ] Quarkus integration

## M10: Cross-Module and Edge Cases — `flagzen-cross-module`

**Status: NOT STARTED** | Depends on: M0

- [ ] Cross-module variant discovery at runtime
- [ ] Package-private variant class support

## M11: Documentation — `flagzen-docs`

**Status: NOT STARTED** | Depends on: M0-M5

- [ ] GitHub Pages site (submodule)
- [ ] Getting started tutorial
- [ ] API reference
- [ ] Architecture guide
- [ ] Provider integration guides
