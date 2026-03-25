# Opportunity Solution Tree -- FlagZen

## Discovery Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Phase**: 2 -- Opportunity Mapping
- **Evidence basis**: Problem validation signals + ecosystem competitive analysis

## Desired Outcome

**Minimize the coupling between feature flag decisions and business logic in Java applications so that flags can be defined, tested, switched, and removed with minimal code impact.**

## Opportunity Solution Tree

```text
Desired Outcome: Decouple flag logic from business logic in Java
|
+-- O1: Unified API across flag providers [Score: 14]
| +-- S1a: SPI-based pluggable backend abstraction                  |
| +-- S1b: OpenFeature-compatible provider layer                    |
| +-- S1c: Adapter pattern with auto-detection (classpath scanning) |
|                                                                   |
+-- O2: Eliminate conditional sprawl from flag checks [Score: 12]
| +-- S2a: @Feature/@Variant polymorphic dispatch                 |
| +-- S2b: Proxy-based runtime resolution                         |
| +-- S2c: Compile-time generated dispatch (annotation processor) |
|                                                                 |
+-- O3: First-class testing support for flag-dependent code [Score: 15]
| +-- S3a: @PinFlag annotation for tests              |
| +-- S3b: Test fixtures with file-based flag sources |
| +-- S3c: Programmatic test context (non-static)     |
|                                                     |
+-- O4: Compile-time safety for flag configurations [Score: 13]
| +-- S4a: Annotation processor validates variant completeness |
| +-- S4b: Enum-based variant validation                       |
| +-- S4c: Dead flag detection at compile time                 |
|                                                              |
+-- O5: Seamless DI framework integration [Score: 11]
| +-- S5a: Spring Boot auto-configuration           |
| +-- S5b: CDI extension                            |
| +-- S5c: Quarkus extension (build-time optimized) |
|                                                   |
+-- O6: Flag lifecycle management (observability) [Score: 9]
| +-- S6a: Usage statistics collection module   |
| +-- S6b: Hotspot detection                    |
| +-- S6c: Compile-time unused variant warnings |
|                                               |
+-- O7: Context-aware flag resolution (A/B testing, multi-tenancy) [Score: 10]
| +-- S7a: Evaluation context with scoping (request, thread, block) |
| +-- S7b: Reactive context propagation (Reactor, Mutiny)           |
| +-- S7c: ScopedValue/ThreadLocal fallback chain                   |
```

## Opportunity Scoring Detail

### Scoring Formula

Score = Importance + Max(0, Importance - Satisfaction)

Importance: How much does this matter to Java devs using flags? (1-10)
Satisfaction: How well do current tools solve this? (1-10)

|  #  |         Opportunity          | Importance | Satisfaction | Score |                                                                                  Rationale                                                                                   |
| --- | ---------------------------- | ---------- | ------------ | ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| O1  | Unified API across providers | 8          | 2            | 14    | OpenFeature exists but has poor DX, no DI integration, verbose. Most teams just use one SDK directly. High importance for multi-provider teams.                              |
| O2  | Eliminate conditional sprawl | 7          | 2            | 12    | Recognized pain, zero solutions exist. But tolerance is high -- devs live with if/else. Importance slightly lower because it's "annoying" not "blocking."                    |
| O3  | First-class testing support  | 9          | 3            | 15    | Highest pain intensity. Every flag library's testing story is weak. Developers feel this friction on every test. Togglz has partial support; others require complex mocking. |
| O4  | Compile-time safety          | 8          | 2            | 14    | Flag name typos cause runtime errors. No existing library validates at compile time. This is the kind of thing Java devs love (type safety is why they chose Java).          |
| O5  | DI framework integration     | 8          | 5            | 11    | Important but partially served. Togglz has Spring integration. LaunchDarkly has Spring Boot starter. Gap is in the abstraction -- integrations are provider-specific.        |
| O6  | Flag lifecycle management    | 6          | 3            | 9     | Nice to have. Some commercial tools (LaunchDarkly) offer this. Less acute pain.                                                                                              |
| O7  | Context-aware resolution     | 7          | 3            | 11    | Important for A/B testing and multi-tenant. Reactive context propagation is genuinely hard and unsolved.                                                                     |

## Top 3 Prioritized Opportunities

### Rank 1: O3 -- First-Class Testing Support (Score: 15)

**Why first**: Highest score. Testing friction is where developers feel the most acute, repeated pain. It's also the strongest adoption wedge -- a library that makes testing trivially easy gets adopted even if people don't use its other features initially.

**Key insight**: If FlagZen's testing story is good enough, developers might adopt it just for tests and gradually use the full API. This is the "trojan horse" strategy.

**Solution ideas to test**:

- S3a: `@PinFlag(feature = "x", variant = "y")` on test methods
- S3b: `@FlagSource("flags-test.properties")` for file-based configs
- S3c: `testFlagContext.pin("x", "y")` for programmatic control

### Rank 2: O1 -- Unified API Across Providers (Score: 14, tied with O4)

**Why second**: Addresses the core structural problem. Multi-provider reality is common and getting more so. OpenFeature validates the need but leaves a DX gap.

**Key insight**: OpenFeature focuses on standardization (good for providers). FlagZen should focus on developer experience (good for consumers). They could be complementary -- FlagZen could use OpenFeature as one backend.

**Solution ideas to test**:

- S1a: SPI-based backend with zero-config for common providers
- S1b: OpenFeature as one supported backend (not competitor, complement)
- S1c: Auto-detection from classpath (add dependency, it works)

### Rank 3: O4 -- Compile-Time Safety (Score: 14, tied with O1)

**Why third**: Differentiator that plays to Java's strengths. No competitor offers this. Annotation processing is the mechanism, and it enables multiple features (variant validation, dead flag detection, proxy generation).

**Key insight**: This is the technical differentiator that makes FlagZen "feel like Java" rather than "a port of a JavaScript pattern." It's what makes the library worth a blog post.

**Solution ideas to test**:

- S4a: Annotation processor that validates all variants are covered
- S4b: Compile error for typos in variant names (when enum exists)
- S4c: Warnings for unused variants

## Opportunities NOT Prioritized (and Why)

|         Opportunity          | Score |                                                     Why Deprioritized                                                     |
| ---------------------------- | ----- | ------------------------------------------------------------------------------------------------------------------------- |
| O2: Polymorphic dispatch     | 12    | Novel but unproven. Risk of being "clever" rather than useful. Include as a feature, but don't lead with it for adoption. |
| O5: DI integration           | 11    | Table stakes, not differentiator. Build it, but it won't drive adoption on its own.                                       |
| O7: Context-aware resolution | 11    | Important for advanced use cases but not the entry point. Phase 2 feature.                                                |
| O6: Flag lifecycle           | 9     | Nice to have. Build after core adoption.                                                                                  |

## Competitive Landscape

|       Tool       |    O1 Unified API    | O2 Poly Dispatch |     O3 Testing      | O4 Compile Safety |    O5 DI     |   O6 Lifecycle   | O7 Context |
| ---------------- | -------------------- | ---------------- | ------------------- | ----------------- | ------------ | ---------------- | ---------- |
| LaunchDarkly SDK | No (single provider) | No               | Weak (test server)  | No                | Partial      | Yes (commercial) | Partial    |
| Togglz           | No (single impl)     | No               | Partial (JUnit ext) | No                | Yes (Spring) | Partial          | No         |
| OpenFeature Java | Yes (core purpose)   | No               | Weak                | No                | No           | No               | Partial    |
| Unleash SDK      | No (single provider) | No               | Weak                | No                | No           | Partial          | No         |
| **FlagZen**      | **Yes**              | **Yes (unique)** | **Yes (unique DX)** | **Yes (unique)**  | **Yes**      | **Yes**          | **Yes**    |

**Key takeaway**: FlagZen's differentiation is real in O2, O3, and O4. O1 overlaps with OpenFeature but with better DX. The combination is what no one else offers.

## Gate G2 Evaluation

|          Criterion           | Status |                          Notes                           |
| ---------------------------- | ------ | -------------------------------------------------------- |
| Opportunities identified: 5+ | PASS   | 7 opportunities mapped                                   |
| Top scores >8                | PASS   | Top 3: 15, 14, 14                                        |
| Job step coverage: 80%+      | PASS   | All 8 job steps from JTBD map are covered                |
| Team alignment               | N/A    | Solo developer project; alignment is with project author |

**G2 Decision: PROCEED**

Top 3 opportunities are clear, well-differentiated from competition, and technically coherent. O3 (testing) as the adoption wedge is a strong strategic bet.
