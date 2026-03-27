# DESIGN Wave Handoff: flagzen-typed-variants

## Summary

This package defines the requirements for Milestone 2 (Typed Variants and Conditional API) of FlagZen. It adds typed polymorphic dispatch (INT, BOOLEAN) and typed accessor methods on FlagProvider for non-polymorphic flag access.

## Artifacts

|         Artifact          |                Path                |                     Purpose                     |
| ------------------------- | ---------------------------------- | ----------------------------------------------- |
| Journey Visual            | `journey-typed-dispatch-visual.md` | ASCII flow with emotional arc and error paths   |
| Journey Schema            | `journey-typed-dispatch.yaml`      | Structured journey with shared artifacts        |
| Gherkin Scenarios         | `journey-typed-dispatch.feature`   | 26 testable scenarios across 2 features         |
| Story Map                 | `story-map.md`                     | Backbone, walking skeleton, 2 releases          |
| Prioritization            | `prioritization.md`                | MoSCoW and release ordering                     |
| User Stories              | `user-stories.md`                  | 7 LeanUX stories with BDD scenarios             |
| Shared Artifacts Registry | `shared-artifacts-registry.md`     | 5 shared artifacts with integration checkpoints |
| Outcome KPIs              | `outcome-kpis.md`                  | 4 KPIs with measurement plan                    |
| DoR Validation            | `dor-validation.md`                | All 7 stories PASSED                            |
| Peer Review               | `peer-review.md`                   | APPROVED after 2 iterations                     |

## Stories for DESIGN Wave

| Story ID |                     Title                      |  Priority   | Est. Days | Dependencies |
| -------- | ---------------------------------------------- | ----------- | --------- | ------------ |
| US-M2-01 | FeatureType Enum and @Feature Type Attribute   | P1 (Must)   | 1         | M0 complete  |
| US-M2-02 | @Variant intValue and booleanValue Attributes  | P1 (Must)   | 1         | US-M2-01     |
| US-M2-03 | Compile-Time Type Consistency Validation       | P1 (Must)   | 2         | US-M2-02     |
| US-M2-04 | Integer Proxy Dispatch and FlagProvider.getInt | P1 (Must)   | 2         | US-M2-03     |
| US-M2-05 | Boolean Dispatch with REQUIRED Completeness    | P2 (Must)   | 1-2       | US-M2-04     |
| US-M2-06 | Conditional API -- getBoolean and getInt       | P3 (Should) | 1         | US-M2-04     |
| US-M2-07 | Conditional API -- getLong and getDouble       | P3 (Could)  | 0.5       | US-M2-06     |

**Total estimated effort**: 8.5-9.5 days

## Key Design Decisions for Solution Architect

1. **Annotation sentinel strategy**: @Variant needs a way to detect which typed attribute was set (intValue vs booleanValue vs value). Java annotations cannot have null defaults. The crafter decides the sentinel approach (e.g., Integer.MIN_VALUE, wrapper type, or separate boolean flags).

2. **getBoolean parsing strictness**: Only exact "true"/"false" (case-insensitive) should parse to boolean. This is intentionally stricter than `Boolean.parseBoolean` which treats everything as false if not "true". Unparseable strings return `Optional.empty()`.

3. **Proxy variant map key type**: INT features use `Map<Integer, Supplier<T>>`, BOOLEAN features use `Map<Boolean, Supplier<T>>`, STRING features continue with `Map<String, Supplier<T>>`. The ProxyGenerator selects the map type based on FeatureType.

4. **FlagProvider method dispatch**: Generated proxy calls the matching FlagProvider method based on FeatureType (getString/getInt/getBoolean). Default methods parse from getString; native providers can override.

5. **Testing DX**: @PinFlag continues to accept string variant values. The proxy's FlagProvider (InMemoryFlagProvider) stores strings, and the proxy parses via default getInt/getBoolean methods. No changes to @PinFlag annotation needed.

## Constraints

- Zero runtime reflection in flagzen-core (unchanged)
- Backward compatible: existing @Feature/@Variant without typed attributes must work identically
- FlagProvider typed methods are default methods -- no breaking change to existing implementations
- All new code in flagzen-core module only

## Risks

|                     Risk                     | Probability | Impact |                                         Mitigation                                         |
| -------------------------------------------- | ----------- | ------ | ------------------------------------------------------------------------------------------ |
| Annotation sentinel detection complexity     | Medium      | Medium | Spike if needed; Java annotation processor APIs support reading default vs explicit values |
| ProxyGenerator code duplication across types | Low         | Low    | Extract dispatch template with type parameter                                              |
| Existing provider adapters need updates      | Low         | Low    | Default methods handle delegation; adapters can optionally override                        |

## DoR Status

All 7 stories PASSED 9-item DoR. Peer review APPROVED (iteration 2).
