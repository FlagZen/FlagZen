# Walking Skeleton Rationale -- flagzen-spring

## Skeleton Selection

Three walking skeletons cover the three essential developer journeys:

1. **Happy path**: FlagProvider bean defined -> auto-configured FeatureDispatcher -> CheckoutFlow proxy injectable -> calling method dispatches to active variant
2. **Fallback path**: No FlagProvider defined -> InMemoryFlagProvider fallback activates -> proxy injectable -> dispatches to default variant
3. **Override path**: Custom FeatureDispatcher bean defined -> auto-configuration backs off -> only the custom dispatcher exists

## Why Three Skeletons

Spring auto-configuration has three distinct modes of operation. Each represents a different developer persona/context:

- **Happy path** = Rafael (senior dev) in production with explicit FlagProvider
- **Fallback path** = Priya (junior dev) onboarding with zero config
- **Override path** = Rafael with custom dispatcher requirements

Each skeleton exercises a different auto-configuration code path end-to-end. All three are thin vertical slices -- they go through context startup, bean creation, and observable outcome.

## Scenarios

| Scenario | Stories | Purpose |
| --- | --- | --- |
| Developer injects a feature proxy and dispatches to the active variant | US-SPRING-01, US-SPRING-02, US-SPRING-03 | Happy path: full auto-config pipeline, observable dispatch |
| Application starts with fallback provider when no explicit provider is defined | US-SPRING-01, US-SPRING-04 | Fallback path: zero-config startup, default variant dispatch |
| Custom dispatcher takes precedence over auto-configured one | US-SPRING-01, US-SPRING-05 | Override path: conditional back-off, custom bean used |

## Walking Skeleton Litmus Test

### Skeleton 1: Happy Path

1. **Title describes user goal**: "Developer injects a feature proxy and dispatches to the active variant" -- yes, the core value proposition.
2. **Given/When describe user actions**: defining a FlagProvider, having a feature interface, starting the app -- yes, user setup actions.
3. **Then describes user observation**: "can inject CheckoutFlow via autowiring" and "executes the Classic variant" -- yes, observable outcomes.
4. **Non-technical stakeholder can confirm**: "Can a developer add flagzen-spring and get working feature flags via @Autowired? Yes." -- passes.

### Skeleton 2: Fallback Path

1. **Title describes user goal**: "Application starts with fallback provider when no explicit provider is defined" -- yes, zero-config onboarding.
2. **Given/When describe user actions**: no provider defined, starting the app -- yes, minimal setup.
3. **Then describes user observation**: "can inject CheckoutFlow" and "dispatches to the default variant" -- yes, observable.
4. **Non-technical stakeholder can confirm**: "Can a new developer start the app without configuring a provider? Yes." -- passes.

### Skeleton 3: Override Path

1. **Title describes user goal**: "Custom dispatcher takes precedence over auto-configured one" -- yes, override capability.
2. **Given/When describe user actions**: defining a custom dispatcher, starting the app -- yes, user action.
3. **Then describes user observation**: "only the custom FeatureDispatcher bean exists" -- yes, observable.
4. **Non-technical stakeholder can confirm**: "Can a senior developer override auto-configuration? Yes." -- passes.

## Not Walking Skeleton (Rationale for Exclusion)

- **Profile-specific provider selection** (US-SPRING-03) -- variant of provider detection, not thinnest slice.
- **Multiple feature proxies** (US-SPRING-02) -- extends single-proxy happy path, not core value.
- **Startup diagnostics** (US-SPRING-06) -- observability enhancement, not core dispatch value.
- **Ambiguous bean error** (US-SPRING-03) -- error path, covered by focused scenarios.
