# FlagZen Progress Tracker

Progress against [project-brief.md](project-brief.md). Each milestone maps to an nWave feature-id for use with `/nw-deliver {feature-id}`, `/nw-design {feature-id}`, etc.

---

## Release Plan

|  Release   |              Milestones               |                                                  Theme                                                   |
| ---------- | ------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **v1.1.0** | M0, M1, M2, M3, M13, M4, M5 (partial) | Core library with typed dispatch, eval context, multi-value, Spring, env provider, one external provider |
| **v1.2.0** | M5 (remaining), M6, M8, M11           | Condition predicates, remaining providers, hooks, cross-module                                           |
| **v1.3.0** | M7, M9                                | Reactive context, extended testing                                                                       |
| **v1.4.0** | M10                                   | CDI/Quarkus integration                                                                                  |

M12 (Documentation) is continuous — Javadoc and docs are updated with every release.

---

## v1.1.0 — Core Library

### M0: Core Polymorphic Dispatch — `flagzen`

**Status: DONE** | **Release: v1.1.0** | [Artifacts](feature/flagzen/)

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

### M1: Evaluation Context — `flagzen-eval-context`

**Status: DONE** | **Release: v1.1.0** | [Artifacts](feature/flagzen-eval-context/)

- [x] `EvaluationContext` model (targeting key + attributes)
- [x] Explicit context parameter on `resolve()`
- [x] Block-scoped context (`FlagContext.run(ctx, () -> ...)`)
- [x] ThreadLocal carrier (ScopedValue deferred to R2)
- [x] `ContextAccessor` SPI implementation
- [x] Resolution order: explicit > accessor > scoped > default
- [x] Default context via `FlagZen.configure()`
- [x] Programmatic accessor registration
- [x] Null context rejection in `FlagContext.run()`

### M2: Typed Variants and Conditional API — `flagzen-typed-variants`

**Status: DONE** | **Release: v1.1.0** | Depends on: M0 | [Artifacts](feature/flagzen-typed-variants/)

#### Typed Polymorphic Dispatch

- [x] `FeatureType` enum (STRING, INT, BOOLEAN) — or inferred from `@Variant` attribute used
- [x] `@Feature(type = FeatureType.INT)` attribute (default STRING for backward compat)
- [x] `@Variant(intValue = 42)` for int-typed features
- [x] `@Variant(booleanValue = true)` for boolean-typed features
- [x] `@WhenTrue` / `@WhenFalse` convenience annotations (with `of` for multi-feature)
- [x] Compile-time validation: all variants of a feature use the same type
- [x] Compile-time validation: `@Variant` attribute matches `@Feature(type = ...)`
- [x] `@Variant(longValue = ...)` for long-typed features
- [x] `@Variant(doubleValue = @CloseTo(value = 0.3))` for double-typed features
- [x] `@CloseTo` annotation with `value` and `delta` (default 1e-10) for approximate double matching
- [x] `FeatureType` extended with LONG and DOUBLE
- [x] Proxy dispatches on typed value (int/long/boolean lookup, double approximate match)
- [x] REQUIRED strategy works with boolean features (exactly 2 variants: true + false)

#### Conditional API (Non-Polymorphic)

- [x] `FlagProvider.getBoolean(String key)` → `Optional<Boolean>`
- [x] `FlagProvider.getInt(String key)` → `OptionalInt`
- [x] `FlagProvider.getLong(String key)` → `OptionalLong`
- [x] `FlagProvider.getDouble(String key)` → `OptionalDouble`
- [x] `FlagProvider.getString(String key)` remains the primitive (typed methods delegate + parse)
- [x] Context-aware overloads for all typed methods

### M3: Environment Variable Provider — `flagzen-env`

**Status: DONE** | **Release: v1.1.0** | Depends on: M0 | [Artifacts](feature/flagzen-env/)

- [x] `EnvironmentVariableFlagProvider` implementing `FlagProvider`
- [x] Key-to-env-var mapping convention (e.g., `checkout-flow` -> `FLAGZEN_CHECKOUT_FLOW`)
- [x] ServiceLoader registration
- [x] `flagzen-key-mapping` module: `FlagKeyParser`, `FlagKeyParsers`, `FlagKeyFormat`, `FlagKeyFormats`
- [x] Built-in parsers: `screamingSnakeCase` (with/without prefix), `camelCase` (with/without prefix)
- [x] Built-in formatters: kebab, snake, camel, pascal, dot, colon case
- [x] Configurable prefix per parser
- [x] Custom lambda parsers and formatters
- [x] `ConflictStrategy` enum (WARN/ERROR) with cardinality-based defaults
- [x] First-access conflict warning

### M13: Multi-Value Variant Mapping — `flagzen-multi-value-variant`

**Status: DONE** | **Release: v1.1.0** | Depends on: M0 | [Artifacts](feature/flagzen-multi-value-variant/)

- [x] `@Variant(value = {"CLASSIC", "LEGACY"})` — `String[] value()` on @Variant (non-breaking: single string still works)
- [x] `@Variant(intValue = {3, 5})` — `int[] intValue()` for multi-value int dispatch
- [x] `@Variant(longValue = {1000L, 5000L})` — `long[] longValue()` for multi-value long dispatch
- [x] Repeated `@Variant` annotations on same class also supported (existing `@Repeatable`)
- [x] Processor registers implementation class under all specified values
- [x] Compile-time duplicate detection across multi-value arrays
- [x] Both syntaxes composable: array values + repeated annotations
- [x] `@CloseTo` overlap detection (inter-variant and intra-variant)

### M4: Spring Integration — `flagzen-spring`

**Status: NOT STARTED** | **Release: v1.1.0** | Depends on: M0

- [ ] `FlagZenAutoConfiguration` (Spring Boot auto-config)
- [ ] `FeatureFactoryBean` for `@Autowired` injection of `@Feature` proxies
- [ ] `FlagProvider` bean auto-detected from `ApplicationContext`
- [ ] `@Variant` + `@Component` classes participate in Spring DI
- [ ] Conditional annotations (`@ConditionalOnMissingBean`)

### M5: Provider Adapters — `flagzen-providers`

**Status: NOT STARTED** | **Release: v1.1.0 (partial), v1.2.0 (remaining)** | Depends on: M0

- [ ] `flagzen-openfeature` (OpenFeature SDK adapter) — **v1.1.0** (vendor-neutral, proves SPI)
- [ ] `flagzen-launchdarkly` (LaunchDarkly SDK adapter) — v1.2.0
- [ ] `flagzen-togglz` (Togglz adapter) — v1.2.0

### M12: Documentation — `flagzen-docs`

**Status: NOT STARTED** | **Release: continuous** | Depends on: M0

Documentation is updated with every release. Each release adds docs for its new features.

#### Documentation Site

- [ ] GitHub Pages site hosted at flagzen.com (custom domain)
- [ ] Documentation repository/submodule with static site generator
- [ ] Getting started tutorial
- [ ] API reference
- [ ] Architecture guide
- [ ] Provider integration guides

#### In-Repo Documentation

- [ ] README with quick start guide
- [ ] Javadoc on all public API types

#### Examples Module — `flagzen-examples`

- [ ] `flagzen-examples` Gradle submodule (not published to Maven Central)
- [ ] Example: basic polymorphic dispatch (`@Feature` + `@Variant` + `FeatureDispatcher`)
- [ ] Example: typed dispatch (INT, BOOLEAN, LONG, DOUBLE with `@CloseTo`)
- [ ] Example: evaluation context (explicit, block-scoped, accessor)
- [ ] Example: testing with `@PinFlag`, `@FlagSource`, `TestFlagContext`
- [ ] Example: Spring Boot integration (`@Autowired` feature injection)
- [ ] Example: custom `FlagProvider` implementation
- [ ] Example: condition predicates (`@Condition(matches = ...)` with JDK predicates)
- [ ] Every example has a corresponding test that compiles and runs

---

## v1.2.0 — Extended Dispatch and Providers

### M6: Condition Predicates — `flagzen-conditions`

**Status: DESIGN DONE** | **Release: v1.2.0** | Depends on: M1 | [Artifacts](feature/flagzen-conditions/)

> **Note**: Condition predicates test the **flag value** (ranges, thresholds, patterns), not the evaluation context. Context-based targeting belongs in the flag provider (LaunchDarkly, OpenFeature, Togglz).

- [ ] `@Condition(matches = X.class)` — predicate class reference
- [ ] `@Condition(notMatches = X.class)` — negated predicate (mutually exclusive with `matches`)
- [ ] `@Variant(when = @Condition(matches = HighRange.class), order = 2)` syntax
- [ ] `order` on `@Variant` (not `@Condition`) — enables mixed exact-match + condition dispatch
- [ ] `order` optional when unambiguous, mandatory when mixed or multiple conditions
- [ ] Predicates use JDK interfaces: `Predicate<String>`, `IntPredicate`, `LongPredicate`, `DoublePredicate`
- [ ] Compile-time validation: predicate type matches `@Feature(type = ...)` (e.g., INT → `IntPredicate`)
- [ ] Exact matches and conditions can coexist on same `@Feature` (unified ordered dispatch, ADR-008)
- [ ] Fallback to `@DefaultVariant` when no match
- [ ] Interaction with `FallbackStrategy` when no match and no default
- [ ] Proxy generation: ordered list evaluation when `order` present, map lookup when absent
- [ ] Predicates instantiated via no-arg constructor (or DI when Spring module present)

### M8: Hooks and Observability — `flagzen-hooks`

**Status: NOT STARTED** | **Release: v1.2.0** | Depends on: M0

- [ ] Hook SPI for dispatch events (metrics, structured logging)
- [ ] Flag usage statistics collection
- [ ] Dead flag detection
- [ ] Compile-time warnings for unused variants
- [ ] Hotspot analysis

### M11: Cross-Module and Edge Cases — `flagzen-cross-module`

**Status: NOT STARTED** | **Release: v1.2.0** | Depends on: M0

- [ ] Cross-module variant discovery at runtime
- [ ] Package-private variant class support

---

## v1.3.0 — Reactive and Extended Testing

### M7: Reactive Context Propagation — `flagzen-reactive`

**Status: NOT STARTED** | **Release: v1.3.0** | Depends on: M1

- [ ] `flagzen-reactor` (Reactor `Context` for Spring WebFlux)
- [ ] `flagzen-mutiny` (Mutiny `Context` for Quarkus Reactive)

### M9: Extended Testing Support — `flagzen-test-extras`

**Status: NOT STARTED** | **Release: v1.3.0** | Depends on: M0

- [ ] `@FlagSource` for JSON format
- [ ] `@FlagSource` for YAML format
- [ ] JUnit 4 extension
- [ ] TestNG extension
- [ ] Test fixtures / helpers

---

## v1.4.0 — Ecosystem Expansion

### M10: Additional DI Frameworks — `flagzen-cdi`

**Status: NOT STARTED** | **Release: v1.4.0** | Depends on: M4

- [ ] CDI integration
- [ ] Quarkus integration
