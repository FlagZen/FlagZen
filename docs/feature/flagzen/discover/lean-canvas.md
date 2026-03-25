# Lean Canvas -- FlagZen

## Discovery Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Phase**: 4 -- Market Viability
- **Adaptation**: Open-source library; "revenue" = adoption, "customers" = developers, "viability" = sustainable community/ecosystem fit

---

## 1. Problem (Validated in Phase 1)

|  #  |                                                  Problem                                                  |                          Evidence Strength                           |
| --- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| P1  | Feature flag implementations are tightly coupled to specific providers, making switching/combining costly | Strong -- OpenFeature existence validates; migration questions on SO |
| P2  | If/else conditional sprawl from flag checks increases complexity, violates OCP, creates dead code         | Moderate -- recognized anti-pattern, high tolerance                  |
| P3  | Testing flag-dependent code requires disproportionate setup compared to the feature being tested          | Strong -- universal complaint across all flag libraries              |

**Existing alternatives**: OpenFeature (abstraction but poor DX), Togglz (Java-native but single-impl), LaunchDarkly SDK (great platform, lock-in), manual strategy pattern (correct but boilerplate-heavy).

## 2. Customer Segments (by JTBD)

|                           Segment                           |                          Job                           |                 Size Estimate                  | Priority  |
| ----------------------------------------------------------- | ------------------------------------------------------ | ---------------------------------------------- | --------- |
| **Backend Java devs using feature flags in production**     | Manage flag lifecycle with minimal coupling            | Large (most Java enterprise teams)             | Primary   |
| **Teams with 2+ flag providers**                            | Unify flag access across providers without vendor lock | Medium (growing as teams adopt multiple tools) | Primary   |
| **Java devs who value type safety and compile-time checks** | Catch flag configuration errors before runtime         | Large (core Java value proposition)            | Secondary |
| **Teams migrating between flag providers**                  | Switch providers with minimal code changes             | Small (episodic but high-pain)                 | Tertiary  |

## 3. Unique Value Proposition

**Single message**: Feature flags as polymorphic types -- type-safe, testable, provider-independent feature flagging for Java with zero runtime reflection.

**Breakdown**:

- For Java developers frustrated by flag boilerplate and provider lock-in
- FlagZen is an open-source feature flag library
- That replaces scattered if/else conditionals with type-safe polymorphic dispatch
- Unlike OpenFeature (verbose API), LaunchDarkly SDK (vendor lock-in), or Togglz (limited abstraction)
- FlagZen provides compile-time validation, first-class testing support, and pluggable backends through standard Java annotation processing

## 4. Solution (Top Features for Top Problems)

|        Problem         |                         Solution Feature                          | Phase |
| ---------------------- | ----------------------------------------------------------------- | ----- |
| P1: Provider coupling  | SPI-based pluggable backend abstraction with auto-detection       | MVP   |
| P2: Conditional sprawl | @Feature/@Variant polymorphic dispatch with generated proxies     | MVP   |
| P3: Testing friction   | @PinFlag, @FlagSource, TestFlagContext -- zero-setup test support | MVP   |

**MVP module structure**:

- `flagzen-core`: Annotations, SPI, annotation processor, proxy generation
- `flagzen-test`: JUnit 5 extension, @PinFlag, @FlagSource, TestFlagContext
- `flagzen-env`: Environment variable backend (simplest provider)
- `flagzen-spring`: Spring Boot auto-configuration

## 5. Channels (Path to Developers)

|                      Channel                       | Effort |                   Expected Impact                    |       Validation Status        |
| -------------------------------------------------- | ------ | ---------------------------------------------------- | ------------------------------ |
| GitHub (README, examples, good first issues)       | Low    | High -- primary discovery channel for Java libs      | Hypothesis -- standard for OSS |
| Blog post: "Feature Flags Without If/Else in Java" | Medium | High -- novel concept drives interest                | Hypothesis                     |
| Conference talk (Devoxx, Spring I/O, JUG meetups)  | High   | High -- credibility + demo opportunity               | Hypothesis                     |
| Maven Central presence                             | Low    | Table stakes -- required for adoption                | Known pattern                  |
| Dev.to / DZone / Baeldung tutorial                 | Medium | High -- Baeldung is where Java devs learn patterns   | Hypothesis                     |
| Twitter/Mastodon Java community                    | Low    | Medium -- awareness, not deep adoption               | Hypothesis                     |
| Reddit r/java, r/programming                       | Low    | Medium -- can drive initial GitHub traffic           | Hypothesis                     |
| OpenFeature community engagement                   | Medium | Medium -- position as complementary, not competitive | Hypothesis                     |

**Primary adoption strategy**: Testing DX as the trojan horse. Lead with "the easiest way to test feature flags in Java" rather than the full abstraction story. Developers adopt for testing, discover the full API over time.

## 6. Revenue Streams (Adapted for OSS)

Traditional revenue is N/A. "Revenue" for an OSS library = sustainable value capture:

|     Value Stream      |                        Description                         |            Status            |
| --------------------- | ---------------------------------------------------------- | ---------------------------- |
| Developer reputation  | Author/contributor credibility in Java ecosystem           | Primary motivator            |
| Ecosystem influence   | Shapes how Java community thinks about feature flags       | Long-term value              |
| Portfolio project     | Demonstrates annotation processing, API design, OSS skills | Immediate value              |
| Potential consulting  | If adopted, consulting on flag architecture                | Speculative                  |
| Potential sponsorship | GitHub sponsors if community grows                         | Speculative, low probability |

**Honest assessment**: This is a passion/craft project with reputational value. It's not a business. That's fine -- most impactful Java libraries (Jackson, Lombok, MapStruct) started the same way.

## 7. Cost Structure

|           Cost           |             Type              |                Estimate                |
| ------------------------ | ----------------------------- | -------------------------------------- |
| Development time         | Primary cost -- author's time | Significant (months of part-time work) |
| CI/CD infrastructure     | GitHub Actions (free for OSS) | Zero                                   |
| Maven Central publishing | Free via Sonatype OSSRH       | Zero                                   |
| Documentation hosting    | GitHub Pages (free)           | Zero                                   |
| Domain (if desired)      | Optional                      | ~$12/year                              |

**Key insight**: The only real cost is time. The question isn't "can we afford this?" but "is the learning and reputational value worth the time investment?"

## 8. Key Metrics

|                 Metric                 |   What It Measures    | Target (Year 1) |
| -------------------------------------- | --------------------- | --------------- |
| GitHub stars                           | Awareness/interest    | 200+            |
| Maven Central downloads/month          | Actual adoption       | 100+            |
| GitHub issues (non-bug)                | Community engagement  | 20+             |
| External blog posts / mentions         | Ecosystem recognition | 5+              |
| Contributors (non-author)              | Community health      | 3+              |
| Projects using FlagZen (GitHub search) | Real-world adoption   | 10+             |

## 9. Unfair Advantage

|                        Advantage                        |                       Copyability                        |       Durability       |
| ------------------------------------------------------- | -------------------------------------------------------- | ---------------------- |
| Compile-time annotation processing (zero reflection)    | Hard -- requires deep annotation processor expertise     | High -- technical moat |
| Polymorphic dispatch pattern                            | Medium -- concept is copyable, execution quality matters | Medium                 |
| First-mover in "type-safe feature flags for Java" niche | Low -- but execution + community matters more            | Medium                 |
| Comprehensive testing DX                                | Medium -- could be copied by existing libraries          | Low-Medium             |

**Honest assessment**: No OSS library has a true unfair advantage. The combination of compile-time safety + polymorphic dispatch + testing DX + pluggable backends is the moat. Any one feature could be copied; the integrated package is harder to replicate.

---

## Four Big Risks Assessment

### Value Risk: Will developers want this?

|                  Signal                  | Status |                                Evidence                                |
| ---------------------------------------- | ------ | ---------------------------------------------------------------------- |
| Problem validated (3 problems confirmed) | GREEN  | Phase 1: 18 signals across 3 problems                                  |
| Solution maps to real workflow           | GREEN  | Phase 3: API design matches familiar patterns                          |
| Differentiation from alternatives        | GREEN  | Unique in O2, O3, O4; complementary to OpenFeature                     |
| Adoption motivation exists               | YELLOW | Developers must add a new dependency -- always a barrier for Java devs |

**Overall**: GREEN -- Value proposition is clear. The dependency-addition barrier is real but manageable with a strong testing story.

### Usability Risk: Can developers use this?

|            Signal             | Status |                           Evidence                           |
| ----------------------------- | ------ | ------------------------------------------------------------ |
| API maps to familiar patterns | GREEN  | Strategy pattern, Spring conventions, standard annotations   |
| Learning curve is acceptable  | GREEN  | Core concepts learnable in <15 min                           |
| Proxy behavior documented     | YELLOW | Runtime switching may surprise; needs explicit documentation |
| Build tool integration smooth | YELLOW | Annotation processor setup requires Gradle/Maven config      |

**Overall**: GREEN -- API design is conventional Java. Build tool setup and proxy behavior need good documentation.

### Feasibility Risk: Can we build this?

|                  Signal                  | Status |                         Evidence                          |
| ---------------------------------------- | ------ | --------------------------------------------------------- |
| Annotation processing is well-understood | GREEN  | Established Java technology (Dagger, MapStruct precedent) |
| Proxy generation is feasible             | GREEN  | Standard Java proxies or code generation                  |
| Cross-module validation                  | YELLOW | Requires hybrid compile-time + runtime approach           |
| DI framework integration                 | GREEN  | FactoryBean/CDI extension patterns are proven             |
| Reactive context propagation             | YELLOW | Complex but scoped to extension modules                   |

**Overall**: YELLOW -- Core is feasible. Cross-module validation and reactive support add complexity. Mitigated by phased delivery (MVP first, extensions later).

### Viability Risk: Can this sustain?

|             Signal             | Status |                      Evidence                       |
| ------------------------------ | ------ | --------------------------------------------------- |
| Cost structure is near-zero    | GREEN  | Only cost is time                                   |
| Maintenance burden sustainable | YELLOW | One-person project; bus factor = 1                  |
| Ecosystem fit is clear         | GREEN  | Fills a real gap in Java tooling                    |
| Community potential            | YELLOW | Niche audience; growth depends on content marketing |

**Overall**: YELLOW -- Sustainable as a passion project. Long-term risk is maintainer burnout if adoption grows faster than contributor base. Mitigated by modular design (community can own extensions).

### Risk Summary

|    Risk     | Status |            Key Concern             |
| ----------- | ------ | ---------------------------------- |
| Value       | GREEN  | Dependency-addition barrier        |
| Usability   | GREEN  | Proxy behavior documentation       |
| Feasibility | YELLOW | Cross-module validation complexity |
| Viability   | YELLOW | Solo maintainer sustainability     |

**All risks at GREEN or YELLOW -- no RED flags. Proceed.**

---

## Gate G4 Evaluation

|       Criterion       |      Status      |                            Notes                            |
| --------------------- | ---------------- | ----------------------------------------------------------- |
| Lean Canvas complete  | PASS             | All 9 sections filled with evidence                         |
| 4 big risks addressed | PASS             | All GREEN or YELLOW; no RED                                 |
| Channel validated     | CONDITIONAL PASS | Channels are hypothesis-based (standard for pre-launch OSS) |
| Stakeholder sign-off  | N/A              | Solo project; author is stakeholder                         |
| Go/No-Go documented   | See below        |                                                             |

## Go / No-Go Decision

**DECISION: GO**

**Rationale**:

1. The problem space is real and validated by ecosystem signals (OpenFeature, migration patterns, testing complaints)
2. The solution is differentiated -- no existing Java library combines compile-time safety, polymorphic dispatch, testing DX, and pluggable backends
3. Technical feasibility is confirmed with known patterns (annotation processing, proxy generation, SPI)
4. Cost is time-only; reputational and learning value justifies the investment regardless of adoption outcome
5. Risk profile is acceptable (all GREEN/YELLOW)

**Conditions**:

- Lead with testing DX for adoption; don't lead with polymorphic dispatch (it's novel but needs proving)
- Ship MVP with limited scope (core + test + env backend + Spring) before expanding
- Validate A1 and A2 (highest-risk assumptions) through early user feedback after initial release
- Design for contributor-friendliness from day one (modular architecture, good first issues, clear contribution guide)

**Key uncertainty**: Will Java developers actually adopt a new dependency for better flag DX, or will they continue tolerating the status quo? This can only be answered by shipping and measuring. The discovery evidence says the problem is real and the solution is sound -- but adoption is never guaranteed.
