# FlagZen Progress Tracker

Progress against [project-brief.md](project-brief.md). Each milestone maps to an nWave feature-id for use with `/nw-deliver {feature-id}`, `/nw-design {feature-id}`, etc.

---

## Release Plan

|  Release   |            Milestones            |                                            Theme                                            |
| ---------- | -------------------------------- | ------------------------------------------------------------------------------------------- |
| **v1.1.0** | M0, M1, M2, M3, M13, M4, M5 (partial) | Core library with typed dispatch, eval context, multi-value, Spring, env provider, one external provider |
| **v1.2.0** | M5 (remaining), M6, M8, M11      | Condition predicates, remaining providers, hooks, cross-module                              |
| **v1.3.0** | M7, M9                           | Reactive context, extended testing                                                          |
| **v1.4.0** | M10                              | CDI/Quarkus integration                                                                     |

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

**Status: NOT STARTED** | **Release: v1.1.0** | Depends on: M0

#### Typed Polymorphic Dispatch

- [ ] `FeatureType` enum (STRING, INT, BOOLEAN) — or inferred from `@Variant` attribute used
- [ ] `@Feature(type = FeatureType.INT)` attribute (default STRING for backward compat)
- [ ] `@Variant(intValue = 42)` for int-typed features
- [ ] `@Variant(booleanValue = true)` for boolean-typed features
- [ ] `@WhenTrue` / `@WhenFalse` convenience annotations (with `of` for multi-feature)
- [ ] Compile-time validation: all variants of a feature use the same type
- [ ] Compile-time validation: `@Variant` attribute matches `@Feature(type = ...)`
- [ ] `@Variant(longValue = ...)` for long-typed features
- [ ] `@Variant(doubleValue = @CloseTo(value = 0.3))` for double-typed features
- [ ] `@CloseTo` annotation with `value` and `delta` (default 1e-10) for approximate double matching
- [ ] `FeatureType` extended with LONG and DOUBLE
- [ ] Proxy dispatches on typed value (int/long/boolean lookup, double approximate match)
- [ ] REQUIRED strategy works with boolean features (exactly 2 variants: true + false)

#### Conditional API (Non-Polymorphic)

- [ ] `FlagProvider.getBoolean(String key)` → `Optional<Boolean>`
- [ ] `FlagProvider.getInt(String key)` → `OptionalInt`
- [ ] `FlagProvider.getLong(String key)` → `OptionalLong`
- [ ] `FlagProvider.getDouble(String key)` → `OptionalDouble`
- [ ] `FlagProvider.getString(String key)` remains the primitive (typed methods delegate + parse)
- [ ] Context-aware overloads for all typed methods

### M3: Environment Variable Provider — `flagzen-env`

**Status: NOT STARTED** | **Release: v1.1.0** | Depends on: M0

- [ ] `EnvironmentVariableFlagProvider` implementing `FlagProvider`
- [ ] Key-to-env-var mapping convention (e.g., `checkout-flow` -> `FLAGZEN_CHECKOUT_FLOW`)
- [ ] ServiceLoader registration

### M13: Multi-Value Variant Mapping — `flagzen-multi-value-variant`

**Status: NOT STARTED** | **Release: v1.1.0** | Depends on: M0

- [ ] `@Variant(value = {"CLASSIC", "LEGACY"})` — `String[] value()` on @Variant (non-breaking: single string still works)
- [ ] `@Variant(intValue = {3, 5})` — `int[] intValue()` for multi-value int dispatch
- [ ] Repeated `@Variant` annotations on same class also supported (existing `@Repeatable`)
- [ ] Processor registers implementation class under all specified values
- [ ] Compile-time duplicate detection across multi-value arrays
- [ ] Both syntaxes composable: array values + repeated annotations

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
- [ ] Example: condition predicates (`@Condition` + `FeaturePredicate`)
- [ ] Every example has a corresponding test that compiles and runs

---

## v1.2.0 — Extended Dispatch and Providers

### M6: Condition Predicates — `flagzen-conditions`

**Status: DESIGN DONE** | **Release: v1.2.0** | Depends on: M1 | [Artifacts](feature/flagzen-conditions/)

> **Note**: When the flag provider supports server-side targeting rules (LaunchDarkly, OpenFeature, Togglz), those are the preferred way to do conditional dispatch. Condition predicates are for pure in-code feature switching where no external flag service is involved — a declarative Strategy pattern selector evaluated against the `EvaluationContext`.

- [ ] `@Condition` annotation (`on = Predicate.class`, `order = int`)
- [ ] `@Variant(when = @Condition(on = IsEnterprise.class, order = 1))` syntax
- [ ] `FeaturePredicate` functional interface (`boolean test(EvaluationContext)`)
- [ ] Predicates evaluated in `order` sequence; first match wins
- [ ] Compile-time validation: predicate class implements `FeaturePredicate`
- [ ] Compile-time validation: mutually exclusive dispatch modes (value-based OR condition-based)
- [ ] Fallback to `@DefaultVariant` when no predicate matches
- [ ] Interaction with `FallbackStrategy` when no predicate matches and no default
- [ ] Proxy generation for predicate-based dispatch
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
