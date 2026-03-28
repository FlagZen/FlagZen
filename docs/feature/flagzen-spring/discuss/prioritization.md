# Prioritization: flagzen-spring

## Release Priority

| Priority |           Release            |                            Target Outcome                             |                                  Rationale                                   |
| -------- | ---------------------------- | --------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| 1        | Walking Skeleton (R1)        | Spring dev can @Autowired a feature proxy and it dispatches correctly | Validates core assumption: auto-config + proxy registration works end-to-end |
| 2        | Safe Defaults (R2)           | Spring dev gets useful behavior even with minimal config              | Fallback provider, conditional guards, diagnostics logging                   |
| 3        | Spring-Managed Variants (R3) | Variant classes participate in Spring DI                              | Deferred to post-v1.1.0. Requires metadata architecture change               |

## Backlog Suggestions

|                     Story                      | Release | Priority | Value | Urgency | Effort | Score |        Dependencies        |
| ---------------------------------------------- | ------- | -------- | ----- | ------- | ------ | ----- | -------------------------- |
| US-SPRING-01: Auto-configure FeatureDispatcher | R1      | P1       | 5     | 5       | 2      | 12.5  | M0 (done)                  |
| US-SPRING-02: Register feature proxy beans     | R1      | P1       | 5     | 5       | 3      | 8.3   | US-SPRING-01               |
| US-SPRING-03: FlagProvider bean detection      | R1      | P1       | 5     | 5       | 2      | 12.5  | None                       |
| US-SPRING-04: InMemoryFlagProvider fallback    | R2      | P2       | 3     | 3       | 1      | 9.0   | US-SPRING-03               |
| US-SPRING-05: ConditionalOnMissingBean guards  | R2      | P2       | 3     | 3       | 1      | 9.0   | US-SPRING-01               |
| US-SPRING-06: Startup diagnostics logging      | R2      | P2       | 2     | 2       | 1      | 4.0   | US-SPRING-01, US-SPRING-02 |

> **Note**: US-SPRING-01 and US-SPRING-03 are tightly coupled in implementation (auto-config class creates dispatcher from provider). They may be delivered as a single story if the combined scope stays under 3 days. US-SPRING-02 depends on the dispatcher bean existing.
