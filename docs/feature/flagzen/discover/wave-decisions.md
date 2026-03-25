# Wave Decisions -- FlagZen

## Discovery Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Decision**: GO -- proceed to DISCUSS wave (product-owner handoff)

## Discovery Summary

|          Phase          |  Status   |                                       Key Finding                                       |
| ----------------------- | --------- | --------------------------------------------------------------------------------------- |
| P1: Problem Validation  | PASS (G1) | 3 validated problems; testing friction and provider lock-in are strongest               |
| P2: Opportunity Mapping | PASS (G2) | 7 opportunities; top 3 scored 15, 14, 14 (testing DX, unified API, compile-time safety) |
| P3: Solution Testing    | PASS (G3) | 5 hypotheses tested; 4/5 proven or mostly proven                                        |
| P4: Market Viability    | PASS (G4) | All risks GREEN or YELLOW; cost is time-only; clear ecosystem gap                       |

## Go / No-Go

**DECISION: GO**

### Reasons to Go

1. **Real problem, no existing solution**: No Java library combines compile-time safety, polymorphic dispatch, testing DX, and pluggable backends. This isn't a "slightly better" play -- it's a genuinely new approach.
2. **Testing DX is a strong adoption wedge**: The testing API is objectively 5-10x less verbose than alternatives. This is measurable, demonstrable, and immediately valuable.
3. **Technical feasibility is high**: All core mechanisms (annotation processing, proxy generation, SPI) are established Java patterns with proven precedent.
4. **Cost is time-only**: No financial risk. Learning and reputational value justifies investment even if adoption is modest.
5. **OpenFeature validates the market**: A CNCF project exists for this problem space, confirming demand. Its DX gaps confirm the opportunity.

### Reasons to Kill (Considered and Rejected)

|                     Concern                      |                                                 Why Rejected                                                 |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------ |
| "Developers won't add a new dependency for this" | Testing DX alone justifies the dependency; multiple value layers reduce the threshold                        |
| "OpenFeature will get better and close the gap"  | OpenFeature focuses on provider standardization, not DX. Different layer. Complementary, not competitive.    |
| "Polymorphic dispatch is too clever"             | It's optional -- devs can use the unified API without polymorphism. It's a power feature, not a requirement. |
| "Solo maintainer can't sustain this"             | Modular design enables community ownership of extensions. Core is small.                                     |

### Key Risks Accepted

1. **Adoption uncertainty** (YELLOW): Will developers actually switch? Can only be answered by shipping.
2. **Cross-module validation complexity** (YELLOW): Hybrid approach needed. Well-understood pattern.
3. **Solo maintainer bus factor** (YELLOW): Mitigated by modular design and good documentation.

## Handoff Deliverables

All artifacts are in `docs/feature/flagzen/discover/`:

|      Artifact      |          File           |                 Status                  |
| ------------------ | ----------------------- | --------------------------------------- |
| Problem Validation | `problem-validation.md` | Complete                                |
| Opportunity Tree   | `opportunity-tree.md`   | Complete                                |
| Solution Testing   | `solution-testing.md`   | Complete                                |
| Lean Canvas        | `lean-canvas.md`        | Complete                                |
| Interview Guide    | `interview-guide.md`    | Complete (interviews not yet conducted) |
| Wave Decisions     | `wave-decisions.md`     | This file                               |

## Recommendations for Product Owner

### MVP Scope (Recommended)

**In MVP**:

- `flagzen-core`: @Feature, @Variant, @DefaultVariant, FallbackStrategy, FlagProvider SPI, annotation processor, proxy generation
- `flagzen-test`: JUnit 5 extension, @PinFlag, @FlagSource, TestFlagContext
- `flagzen-env`: Environment variable backend
- `flagzen-spring`: Spring Boot auto-configuration

**Not in MVP** (Phase 2):

- Additional backends (LaunchDarkly, Togglz, OpenFeature adapters)
- CDI/Quarkus integration
- Reactive context propagation
- Flag lifecycle/observability module
- Flag usage statistics

### Adoption Strategy

1. **Lead with testing**: Blog posts and README should showcase @PinFlag first. It's the most immediately relatable pain point.
2. **Position as complementary to OpenFeature**: Not a competitor. FlagZen can use OpenFeature as a backend.
3. **Target Baeldung or similar for a tutorial**: This is where Java developers discover libraries.
4. **Ship a compelling example project**: A Spring Boot app with multiple flag scenarios.

### Open Questions for Product Owner

1. **Module naming**: `flagzen-core`, `flagzen-test`, etc. vs. flat package structure?
2. **Java version floor**: 17+ as stated in brief, or consider 11+ for broader adoption?
3. **Gradle vs. Maven for the library build**: Brief says Gradle monorepo. Consider publishing Maven BOM for consumers?
4. **OpenFeature relationship**: Implement as a backend adapter, or stay independent?
5. **Documentation approach**: GitHub Pages (stated in brief) vs. single comprehensive README for MVP?

## Evidence Gaps (Honest Assessment)

|                               Gap                               |                                                 Impact                                                 |                                 Mitigation                                 |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------- |
| No direct developer interviews conducted                        | Medium -- ecosystem signals are strong but interviews would increase confidence                        | Interview guide provided; conduct 5+ before heavy investment               |
| Polymorphic dispatch appeal is unvalidated with real developers | Medium -- it's novel and might be "too clever"                                                         | Lead with testing DX; polymorphism is a power feature, not the entry point |
| Adoption metrics are speculative                                | Low -- standard uncertainty for pre-launch OSS                                                         | Define success metrics, measure after launch                               |
| No competitive response analysis                                | Low -- competitors are commercial platforms or CNCF projects; unlikely to pivot toward FlagZen's niche | Monitor OpenFeature evolution                                              |

## State Tracking

```yaml
current_phase: "4 (complete)"
phase_started: "2026-03-25"
interviews_completed:
  phase_1: 0 (ecosystem evidence used)
  phase_2: 0
  phase_3: 0
  phase_4: 0
assumptions_tracked: 10 (A1-A10, see problem-validation.md)
opportunities_identified: 7 (O1-O7, see opportunity-tree.md)
decision_gates_evaluated:
  G1: PASS (conditional -- ecosystem evidence, not interviews)
  G2: PASS
  G3: PASS
  G4: PASS
artifacts_created:
  - docs/feature/flagzen/discover/problem-validation.md
  - docs/feature/flagzen/discover/opportunity-tree.md
  - docs/feature/flagzen/discover/solution-testing.md
  - docs/feature/flagzen/discover/lean-canvas.md
  - docs/feature/flagzen/discover/interview-guide.md
  - docs/feature/flagzen/discover/wave-decisions.md
```
