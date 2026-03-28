# Wave Decisions -- flagzen-env DISCUSS

## Context

- **Feature ID**: flagzen-env
- **Date**: 2026-03-28
- **Wave**: DISCUSS (product-owner)
- **Prior wave**: None (scoped directly from project brief, M3)
- **Revision**: Major redesign -- replaced KeyMapper (output direction) with FlagKeyParser (input) + FlagKeyFormat (output) + ConflictStrategy + eager loading

## DISCUSS Summary

| Phase | Status | Key Output |
| --- | --- | --- |
| Phase 1: Discovery | SKIPPED | Lightweight -- scope well-understood from project brief + revised design spec |
| Phase 2: Journey Visualization | COMPLETE | DX journey (4 steps), emotional arc (curious to satisfied), 2 personas, parse-format pipeline diagram |
| Phase 2.5: Story Mapping | COMPLETE | Story map with backbone (4 activities), 3 releases, walking skeleton = R1 (5 stories) |
| Phase 2.7: Scope Assessment | PASS | 10 stories, 1 bounded context, estimated 6-8 days |
| Phase 3: Coherence Validation | PASS | Shared artifacts registry (8 artifacts), 13 integration checkpoints |
| Phase 4: Requirements Crafting | COMPLETE | 10 user stories, all DoR passed, outcome KPIs defined |
| Phase 5: Peer Review | PENDING | Review required for redesigned scope |

## Decision: PROCEED TO PEER REVIEW (redesigned scope)

## Rename: EnvVarParser -> FlagKeyParser (and Module Split)

**Date**: 2026-03-28

**Rename rationale**: `EnvVarParser` was specific to environment variables, but the parse/format pipeline (source name -> segments -> flag key) is reusable across any provider: file-based, vault, consul, etc. `FlagKeyParser` is provider-agnostic. The same applies to `EnvVarParsers` -> `FlagKeyParsers`.

**Module split**: The key-mapping infrastructure is extracted into a separate `flagzen-key-mapping` module:

| Module | Contains | Package |
| --- | --- | --- |
| flagzen-key-mapping | FlagKeyParser, FlagKeyParsers, FlagKeyFormat, FlagKeyFormats, ConflictStrategy | `com.flagzen.keymapping` |
| flagzen-env | EnvironmentVariableFlagProvider, builder, ServiceLoader registration | `com.flagzen.env` |

**Why split**: `flagzen-key-mapping` is reusable without pulling in the env var provider. Future providers (flagzen-file, flagzen-vault, etc.) can depend on `flagzen-key-mapping` directly for key parsing and formatting, without depending on `flagzen-env`. This prevents a false dependency chain where every provider depends on the env var provider just to get parsers and formatters.

**Story mapping**: US-ENV-05 (parsers), US-ENV-06 (formatters), and US-ENV-09 (ConflictStrategy) target `flagzen-key-mapping`. All other stories target `flagzen-env`.

**User impact**: None. Developers add `flagzen-env` and get `flagzen-key-mapping` transitively. The import changes from `com.flagzen.env.FlagKeyParsers` to `com.flagzen.keymapping.FlagKeyParsers`, but since this is a new API (not a rename of a shipped API), there is no migration cost.

## Design Changes from Previous Version

| Aspect | Old Design | New Design | Rationale |
| --- | --- | --- | --- |
| Direction | KeyMapper: flag key -> env var name (output) | FlagKeyParser: env var name -> segments (input) + FlagKeyFormat: segments -> flag key (output) | Clean separation of parsing and formatting concerns |
| Prefix | Global on provider | Per-parser | Multiple parsers can have different prefixes |
| Loading | Lazy (System.getenv per call) | Eager (all env vars read once at construction) | Predictable, fast, thread-safe |
| Map | None (computed per call) | Immutable map built at construction | O(1) getString(), no runtime I/O |
| Conflict | None | ConflictStrategy (WARN/ERROR) with cardinality-based defaults | Explicit handling for multi-parser/multi-formatter |
| Access warning | None | First-access warning for conflicted keys | Conflict visibility at point of use |
| Stories | 6 (US-ENV-01 through US-ENV-06) | 10 (US-ENV-01 through US-ENV-10) | Finer-grained, focused on new concepts |

## Wave Decisions Applied

| Decision | Choice | Rationale |
| --- | --- | --- |
| Backend/Infrastructure | Yes | FlagProvider implementation + parser/formatter interfaces, no UI |
| Walking Skeleton needed | Yes | R1 (5 stories) is atomic; R2/R3 add independent capabilities |
| UX research depth | Lightweight | Library API design, patterns well-understood |
| JTBD analysis | Skipped | Motivations clear: 12-factor apps + multi-convention codebases |
| Release slicing | 3 releases | R1: defaults, R2: custom config, R3: multi-convention + conflict |

## Artifacts Produced

| Artifact | File | Status |
| --- | --- | --- |
| Journey Visual | `journey-developer-integration-visual.md` | Rewritten (parse-format pipeline diagram) |
| Journey Schema | `journey-developer-integration.yaml` | Rewritten (new shared artifacts) |
| Journey Gherkin | `journey-developer-integration.feature` | Rewritten (new scenarios) |
| Shared Artifacts Registry | `shared-artifacts-registry.md` | Rewritten (8 artifacts, 13 checkpoints) |
| Story Map | `story-map.md` | Rewritten (3 releases, 10 stories) |
| Prioritization | `prioritization.md` | Rewritten (10 stories) |
| User Stories (10) | `user-stories.md` | Rewritten, all DoR passed |
| Outcome KPIs | `outcome-kpis.md` | Rewritten (10 KPIs) |
| DoR Validation | `dor-validation.md` | All 10 PASSED |
| Peer Review | `peer-review.md` | Pending review |
| Wave Decisions | `wave-decisions.md` | This file |

## Handoff Package for DESIGN Wave (solution-architect)

### What the solution-architect receives

1. **Journey artifacts**: Developer integration journey (4 steps), 2 personas (Kenji, Mei-Lin), emotional arc, error paths, parse-format pipeline diagram, built-in parser/formatter reference
2. **Story map**: 10 stories across 3 releases; R1 = walking skeleton (5 stories), R2 = custom config (1 story), R3 = multi-convention + conflict (4 stories)
3. **User stories**: 10 fully specified stories with BDD scenarios, acceptance criteria, real domain examples, and technical notes
4. **Shared artifacts registry**: 8 tracked artifacts, 13 integration checkpoints
5. **Outcome KPIs**: Zero-config activation, eager loading, parse/format pipeline correctness, conflict detection

### What the solution-architect should decide

1. **Package structure**: Confirm `com.flagzen.keymapping` for parsers, formatters, conflict strategy (flagzen-key-mapping) and `com.flagzen.env` for the provider (flagzen-env)
2. **FlagKeyParser interface design**: SAM interface shape, `@FunctionalInterface` annotation, `Optional<List<String>>` return type
3. **FlagKeyFormat interface design**: SAM interface shape, `@FunctionalInterface` annotation
4. **FlagKeyParsers companion class**: Static factory methods for screamingSnakeCase and camelCase
5. **FlagKeyFormats companion class**: Static factory methods for 6 formatters
6. **ConflictStrategy enum design**: Pattern reference: `FallbackStrategy` in flagzen-core
7. **Builder design**: `EnvironmentVariableFlagProvider.builder()` with `.parser()`, `.formatter()`, `.onConflict()`, `.build()`
8. **System.getenv() testability**: Inject `Supplier<Map<String, String>>` or use other test isolation technique
9. **Immutable map implementation**: `Map.copyOf()` or `Collections.unmodifiableMap()`
10. **First-access warning thread safety**: Concurrent access to conflicted-key tracking sets
11. **Null/empty argument handling**: Guard clauses on builder methods
12. **Gradle module setup**: `flagzen-key-mapping/build.gradle` with dependency on `flagzen-core`; `flagzen-env/build.gradle` with dependencies on `flagzen-core` and `flagzen-key-mapping`
13. **Javadoc**: All public API types must have Javadoc (CLAUDE.md requirement)

### What is explicitly NOT decided (solution-neutral)

- Internal implementation details of parsing/formatting algorithms
- Whether to use `Map.copyOf()` vs `Collections.unmodifiableMap()`
- Test framework specifics for env var mocking
- Internal structure of conflict tracking (Set vs ConcurrentHashMap)
- Logging framework (SLF4J vs java.util.logging)
- Whether builder validates at `build()` time or lazily

## Release Plan

| Release | Stories | Estimated Effort | Target Outcome |
| --- | --- | --- | --- |
| R1 | US-ENV-01, US-ENV-02, US-ENV-03, US-ENV-05, US-ENV-06 | 3-4 days | Complete env var provider, zero-config, auto-discoverable, eager loading |
| R2 | US-ENV-04 | 1 day | Custom parser configuration via builder |
| R3 | US-ENV-07, US-ENV-08, US-ENV-09, US-ENV-10 | 2-3 days | Multi-convention support with conflict handling |

## Risk Register

| Risk | Probability | Impact | Mitigation |
| --- | --- | --- | --- |
| System.getenv() hard to mock in tests | Medium | Low | Solution-architect chooses injection strategy |
| camelCase parser edge cases (acronyms like XMLParser) | Medium | Low | Document supported patterns; edge cases rare for env var names |
| Conflict strategy cardinality rules too surprising | Low | Medium | Clear documentation + sensible defaults + explicit override |
| Eager loading reads too many env vars (large environments) | Low | Low | Prefix-based parsers filter aggressively; most env vars excluded |
| Multi-parser + multi-formatter cartesian explosion | Medium | Medium | Default ERROR strategy for multi-multi prevents silent issues |
| Thread safety of first-access warning tracking | Low | Low | Solution-architect decides concurrent data structure |
