# Problem Validation -- FlagZen

## Discovery Context

- **Feature ID**: flagzen
- **Date**: 2026-03-25
- **Phase**: 1 -- Problem Validation
- **Evidence basis**: Public developer signals (GitHub issues, blog posts, Stack Overflow, conference talks, ecosystem analysis) + project author's domain experience. No direct Mom Test interviews conducted.
- **Evidence quality**: Mixed. Strong signals from ecosystem patterns; hypothetical on specific pain intensity.

## Problem Statement (Hypothesis)

Java developers working with feature flags face three distinct problems:

1. **Vendor lock-in and abstraction gap**: Feature flag implementations are tightly coupled to specific providers (LaunchDarkly SDK, Togglz API, etc.), making it costly to switch, combine, or test independently.
2. **Conditional sprawl**: Feature flags implemented via if/else conditionals scatter branching logic throughout the codebase, violating OCP, complicating testing, and creating dead code risk.
3. **Testing friction**: Pinning flag values in tests requires provider-specific setup, mocking, or infrastructure dependencies that make flag-dependent code harder to test than it should be.

## Evidence Assessment

### Problem 1: Vendor Lock-in and Abstraction Gap

**Signal strength**: STRONG (7+ signals)

|  #  |                                                                  Signal                                                                   |       Source       |                         Type                         |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------- | ------------------ | ---------------------------------------------------- |
| 1   | OpenFeature project exists (CNCF sandbox) specifically to solve provider abstraction                                                      | Ecosystem          | Past behavior -- community built a standard for this |
| 2   | Togglz, Unleash, Flipt, and LaunchDarkly all have incompatible Java SDKs                                                                  | Ecosystem analysis | Structural evidence                                  |
| 3   | Teams frequently ask "how to migrate from X to Y" on Stack Overflow for flag providers                                                    | Stack Overflow     | Past behavior                                        |
| 4   | OpenFeature adoption is slow despite solving the right problem -- indicates the abstraction layer exists but the DX isn't good enough yet | GitHub metrics     | Past behavior                                        |
| 5   | Spring Boot does not provide a native feature flag abstraction despite abstracting everything else (data, messaging, caching)             | Framework analysis | Structural gap                                       |
| 6   | Enterprise teams commonly use 2+ flag sources (env vars for infra, LaunchDarkly for product, config files for dev)                        | Industry pattern   | Past behavior                                        |
| 7   | OpenFeature Java SDK requires manual provider registration, no annotation support, no DI integration                                      | API analysis       | Structural limitation                                |

**Assessment**: Problem is real. OpenFeature's existence validates the need. The question is whether OpenFeature's limitations leave enough room for FlagZen.

### Problem 2: Conditional Sprawl

**Signal strength**: MODERATE (5 signals)

|  #  |                                                           Signal                                                           |            Source            |                       Type                        |
| --- | -------------------------------------------------------------------------------------------------------------------------- | ---------------------------- | ------------------------------------------------- |
| 1   | "Feature flag spaghetti" is a recognized anti-pattern discussed in multiple conference talks (Martin Fowler, Pete Hodgson) | Conference talks, blog posts | Community recognition                             |
| 2   | Feature toggles article on martinfowler.com explicitly warns about conditional complexity                                  | Industry literature          | Expert opinion                                    |
| 3   | Strategy pattern for feature flags is discussed but rarely implemented due to boilerplate                                  | Blog posts                   | Past behavior (workaround exists but not adopted) |
| 4   | Static analysis tools (SonarQube) flag high cyclomatic complexity from flag conditionals                                   | Tooling                      | Indirect signal                                   |
| 5   | No existing Java library offers polymorphic dispatch for feature flags                                                     | Ecosystem gap                | Structural                                        |

**Assessment**: Problem is recognized but may be a "nice to have" rather than a "hair on fire" problem. Developers complain about it but tolerate it. The polymorphic dispatch approach is novel -- which means it's either brilliant or a solution looking for a problem. Needs validation.

**Challenge**: Most developers have internalized if/else for flags. The question isn't "is conditional sprawl bad?" (yes) but "is it bad enough that developers will learn a new abstraction to avoid it?" Past behavior suggests tolerance is high.

### Problem 3: Testing Friction

**Signal strength**: STRONG (6 signals)

|  #  |                                            Signal                                             |        Source        |            Type            |
| --- | --------------------------------------------------------------------------------------------- | -------------------- | -------------------------- |
| 1   | LaunchDarkly test support requires running their test server or complex mocking               | Documentation        | Structural friction        |
| 2   | Togglz provides JUnit 5 extension but it's tightly coupled to Togglz internals                | API analysis         | Partial solution exists    |
| 3   | Developers frequently ask about testing feature-flagged code on Stack Overflow                | Stack Overflow       | Past behavior              |
| 4   | Common workaround: extract flag check into injectable service, mock the service               | Code patterns        | Past behavior (workaround) |
| 5   | Test setup for flag-dependent code is disproportionately complex vs. the feature being tested | Developer experience | Structural                 |
| 6   | No standard `@PinFlag` or test-fixture pattern exists across flag libraries                   | Ecosystem gap        | Structural                 |

**Assessment**: Strong signal. Testing is where developers feel the most acute pain because it's where coupling creates the most friction. This could be FlagZen's trojan horse for adoption.

## Customer Words (Paraphrased from Public Sources)

- "Every time we switch flag providers, we touch 200+ files" (migration pain)
- "Our codebase has if/else blocks for flags that were removed months ago" (dead code)
- "I spent more time setting up the flag mock than writing the actual test" (testing friction)
- "We use LaunchDarkly for A/B tests but env vars for infrastructure flags -- two completely different APIs" (multi-provider)
- "Feature flags are supposed to reduce risk, but they add complexity" (irony of the tool)

## Jobs-to-Be-Done Map

**Core Job**: When implementing and managing feature flags in a Java application, minimize the coupling between flag decisions and business logic so that flags can be changed, tested, and removed with minimal code impact.

| Job Step |                       Desired Outcome                       |                          Current Pain                          |
| -------- | ----------------------------------------------------------- | -------------------------------------------------------------- |
| Define   | Minimize time to define a new feature flag and its variants | Must learn provider-specific API; no standard pattern          |
| Locate   | Minimize time to find all code affected by a flag           | Conditionals scattered; grep is the only tool                  |
| Prepare  | Minimize effort to set up flag infrastructure in dev/test   | Provider SDKs require config, connections, API keys            |
| Confirm  | Minimize uncertainty about flag configuration correctness   | Runtime errors for typos in flag names; no compile-time checks |
| Execute  | Minimize boilerplate to branch on flag values               | if/else repeated; strategy pattern requires manual wiring      |
| Monitor  | Minimize effort to track which flags are active and where   | No built-in usage tracking; manual audits                      |
| Modify   | Minimize effort to change flag provider or add new one      | Vendor-locked API calls throughout codebase                    |
| Conclude | Minimize effort to remove a flag after rollout complete     | Must find and delete all conditionals; risky                   |

## Assumption Tracker

|  #  |                                         Assumption                                         |  Category   | Impact (x3) | Uncertainty (x2) | Ease (x1) | Score |  Priority  |
| --- | ------------------------------------------------------------------------------------------ | ----------- | ----------- | ---------------- | --------- | ----- | ---------- |
| A1  | Java devs with 2+ flag sources feel enough pain to adopt a new library                     | Value       | 3 (9)       | 3 (6)            | 2 (2)     | 17    | Test first |
| A2  | Polymorphic dispatch is a compelling enough DX improvement over if/else                    | Value       | 3 (9)       | 3 (6)            | 2 (2)     | 17    | Test first |
| A3  | Compile-time annotation processing (zero reflection) matters to target audience            | Value       | 2 (6)       | 2 (4)            | 1 (1)     | 11    | Test soon  |
| A4  | Developers will learn a new annotation-based API for flag management                       | Usability   | 3 (9)       | 2 (4)            | 2 (2)     | 15    | Test first |
| A5  | Spring/CDI/Quarkus integration is table stakes for adoption                                | Value       | 2 (6)       | 1 (2)            | 1 (1)     | 9     | Test soon  |
| A6  | Testing support (@PinFlag, test fixtures) can drive initial adoption                       | Value       | 2 (6)       | 2 (4)            | 1 (1)     | 11    | Test soon  |
| A7  | OpenFeature leaves enough DX gaps for FlagZen to differentiate                             | Value       | 3 (9)       | 2 (4)            | 2 (2)     | 15    | Test first |
| A8  | Zero-reflection compile-time processing is technically feasible for all described features | Feasibility | 3 (9)       | 2 (4)            | 3 (3)     | 16    | Test first |
| A9  | Developers will accept proxy-based resolution (hidden conditionals) vs explicit if/else    | Usability   | 2 (6)       | 2 (4)            | 2 (2)     | 12    | Test soon  |
| A10 | Open-source library can achieve critical mass without marketing budget                     | Viability   | 2 (6)       | 2 (4)            | 3 (3)     | 13    | Test first |

## Gate G1 Evaluation

|         Criterion         |               Status                |                                              Notes                                               |
| ------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------------------ |
| 5+ evidence signals       | PASS (18 signals across 3 problems) | Sourced from ecosystem analysis, not direct interviews                                           |
| >60% confirm pain         | CONDITIONAL PASS                    | All 3 problems have public evidence; Problem 1 and 3 are strong; Problem 2 needs more validation |
| Problem in customer words | PASS                                | Multiple public quotes paraphrased                                                               |
| 3+ examples               | PASS                                | OpenFeature, Togglz, LaunchDarkly migration patterns all demonstrate the problem                 |

**G1 Decision: PROCEED with caveat**

The problems are real. The caveat: evidence comes from ecosystem analysis, not direct developer interviews. Before significant development investment, validate A1 (multi-provider pain) and A2 (polymorphic dispatch appeal) through direct conversations with 5+ Java developers who currently use feature flags.

**Strongest signal**: The existence of OpenFeature as a CNCF project validates the abstraction need. Its slow adoption and DX gaps validate the opportunity for a better developer experience.
