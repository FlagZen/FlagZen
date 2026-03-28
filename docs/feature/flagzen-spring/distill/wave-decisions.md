# Wave Decisions -- flagzen-spring DISTILL

## Context

- **Feature ID**: flagzen-spring
- **Date**: 2026-03-28
- **Wave**: DISTILL (acceptance-designer)
- **Prior wave**: DESIGN (solution-architect) -- complete

## Decisions

### Decision 1: Feature Scope -- Cross-Cutting (Spring DI + FlagZen Core)

flagzen-spring is a driving adapter module bridging FlagZen core with Spring Boot DI. Tests exercise the auto-configuration lifecycle: `FlagProvider` detection, `FeatureDispatcher` creation, feature proxy registration, and `@Autowired` injection.

### Decision 2: Test Framework -- Cucumber with Spring Boot Test

Project uses Cucumber with JUnit Platform. Feature files in `tests/acceptance/flagzen-spring/`. Step definitions in `com.flagzen.acceptance.steps` (shared glue path). Step definitions use `@CucumberContextConfiguration` + `@SpringBootTest` for real Spring ApplicationContext.

### Decision 3: Integration Approach -- Real Spring ApplicationContext

Tests boot a real Spring ApplicationContext via `@SpringBootTest`. Different configurations per scenario group use `@TestConfiguration` inner classes. `@DirtiesContext` where context must be rebuilt (e.g., testing different bean overrides).

### Decision 4: Infrastructure Testing -- No

Functional acceptance tests only. No Gradle build verification, no JAR content validation, no CI pipeline tests.

### Decision 5: Walking Skeleton Scope

Three walking skeletons covering the three essential paths:

1. **Happy path**: FlagProvider bean -> auto-configured dispatcher -> injected proxy -> correct variant dispatches
2. **Fallback path**: No FlagProvider -> InMemoryFlagProvider fallback -> proxy dispatches to default variant
3. **Override path**: Custom FeatureDispatcher -> auto-configuration backs off

Walking skeletons are NOT tagged `@pending` -- they run immediately.

### Decision 6: File Organization

Feature files organized by business capability:

- `walking-skeleton.feature` -- 3 scenarios (run immediately)
- `auto-configuration.feature` -- 6 scenarios (dispatcher creation, provider detection)
- `feature-proxy-injection.feature` -- 5 scenarios (proxy registration, injection, dynamic dispatch)
- `fallback-provider.feature` -- 7 scenarios (fallback, warnings, conditional guards)
- `startup-diagnostics.feature` -- 3 scenarios (INFO/DEBUG logging)

### Decision 7: Tag Strategy

- Walking skeletons: `@walking-skeleton`, no `@pending` tag, run immediately
- All other scenarios: `@pending` tag, enabled one-at-a-time during DELIVER
- Story traceability: `@US-SPRING-01` through `@US-SPRING-06` on every scenario

### Decision 8: Driving Port Identification

All scenarios exercise through these driving ports only:

| Driving Port | Used In |
| --- | --- |
| Spring `ApplicationContext` | All scenarios -- boot context, retrieve beans |
| `@Autowired` injection | Walking skeleton, proxy injection scenarios |
| `FeatureDispatcher` bean | Auto-configuration, proxy registration scenarios |
| `FlagProvider` bean | Provider detection, fallback scenarios |
| Feature proxy method calls | Walking skeleton, dynamic dispatch scenarios |

No internal classes (`FlagZenAutoConfiguration` internals, `FeatureProxyRegistrar` internals) are tested directly. Tests observe beans in the context and call methods on injected proxies.

### Decision 9: DEVOPS Graceful Degradation

DEVOPS artifacts not created. Default: single Spring Boot test context. No environment matrix needed -- Spring context is programmatically configured via `@TestConfiguration`.

### Decision 10: Spring-Specific Testing Considerations

- `@SpringBootTest` boots real ApplicationContext (not `ApplicationContextRunner`)
- `@TestConfiguration` for per-scenario bean overrides
- `@DirtiesContext` when context state must be isolated between scenarios
- Test `@Feature` interface (e.g., `CheckoutFlow`) with generated metadata must exist on test classpath
- Step definitions need `@CucumberContextConfiguration` for Spring integration

## Artifact Inventory

| Artifact | Path | Status |
| --- | --- | --- |
| Walking skeleton | `tests/acceptance/flagzen-spring/walking-skeleton.feature` | Complete |
| Auto-configuration | `tests/acceptance/flagzen-spring/auto-configuration.feature` | Complete |
| Feature proxy injection | `tests/acceptance/flagzen-spring/feature-proxy-injection.feature` | Complete |
| Fallback provider | `tests/acceptance/flagzen-spring/fallback-provider.feature` | Complete |
| Startup diagnostics | `tests/acceptance/flagzen-spring/startup-diagnostics.feature` | Complete |
| Scenario inventory | `docs/feature/flagzen-spring/distill/test-scenarios.md` | Complete |
| Walking skeleton rationale | `docs/feature/flagzen-spring/distill/walking-skeleton.md` | Complete |
| Wave decisions | `docs/feature/flagzen-spring/distill/wave-decisions.md` | This file |

## Handoff to DELIVER

### Mandate Compliance Evidence

- **CM-A (Hexagonal Boundary)**: All scenarios invoke through driving ports: Spring `ApplicationContext` (boot, retrieve beans), `@Autowired` injection, `FeatureDispatcher` bean, `FlagProvider` bean, feature proxy method calls. Zero internal component references in Gherkin. No references to `FlagZenAutoConfiguration` internals, `FeatureProxyRegistrar`, or `ImportBeanDefinitionRegistrar`.
- **CM-B (Business Language)**: Gherkin uses domain terms only: "application starts", "feature proxy", "flag provider", "feature dispatcher", "autowiring", "default variant", "fallback provider", "startup summary". Zero technical jargon: no HTTP, JSON, SQL, class names, or method signatures in scenario text.
- **CM-C (User Journey)**: 3 walking skeletons (user value E2E) + 21 focused scenarios (boundary tests). Walking skeletons prove "developer adds flagzen-spring, gets injectable feature proxies."
- **CM-D (Pure Function Extraction)**: flagzen-spring is pure adapter code (wiring only, no business logic). No pure functions to extract. `@SpringBootTest` exercises the real auto-configuration lifecycle. No fixture parametrization needed.

### Implementation Sequence (One-at-a-Time)

1. Walking skeleton (3 scenarios -- run first, prove Spring Boot test infrastructure works)
2. Auto-configuration: explicit provider, back-off, imports discovery, profiles, ambiguous beans, cross-module provider
3. Feature proxy injection: single proxy, multiple proxies, constructor injection, dynamic dispatch, no metadata
4. Fallback provider: fallback creation, warning log, default dispatch, no fallback, custom provider, custom dispatcher, full override
5. Startup diagnostics: summary with features, zero features, debug logging

### Build Configuration Note

The `tests/acceptance/flagzen-spring` path must be added to the Cucumber features configuration in `flagzen-acceptance-tests/build.gradle.kts` during DELIVER. Spring Boot Test dependencies (`spring-boot-starter-test`) must be added to test classpath.
