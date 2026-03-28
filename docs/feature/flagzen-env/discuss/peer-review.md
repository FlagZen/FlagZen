# Peer Review -- flagzen-env (Redesigned: Parser/Formatter Separation)

## Review History

### Iteration 1 (Original Scope -- 3 stories)

Approved. Superseded by expanded scope.

### Iteration 2 (Expanded Scope -- 6 stories with KeyMapper)

Approved. Superseded by redesign.

---

## Review Iteration 3 (Redesigned Scope -- 10 stories with FlagKeyParser/FlagKeyFormat)

```yaml
review_id: "req_rev_20260328_003"
reviewer: "product-owner (review mode)"
artifact: "docs/feature/flagzen-env/discuss/user-stories.md (redesigned: 10 stories)"
iteration: 3

strengths:
  - "Clean separation of concerns: parsing (input) and formatting (output) are independent, composable SAM interfaces"
  - "Eager loading with immutable map is well-motivated (US-ENV-02) with concrete performance rationale"
  - "ConflictStrategy cardinality rules are explicit and well-documented -- multi×multi defaults to ERROR is the safe choice"
  - "First-access warning (US-ENV-10) addresses a real developer pain point: construction-time warnings lost in log noise"
  - "Prefix is per-parser, not global -- enables clean multi-parser configurations where each parser has its own prefix"
  - "All 10 stories have concrete personas, real data examples, and focused BDD scenarios"
  - "Release slicing is outcome-based: R1 = zero-config works, R2 = custom config, R3 = multi-convention + conflict"
  - "Journey visual includes clear ASCII pipeline diagram showing parse-format flow"
  - "Builder API examples in journey visual show all configuration modes (create(), custom prefix, multi-parser, lambda)"
  - "Story dependencies are tracked accurately across all 10 stories"

issues_identified:
  confirmation_bias:
    - issue: "All parsers assume lowercase segment normalization. No scenario for a parser that preserves case in segments (e.g., a case-sensitive flag key system)."
      severity: "low"
      location: "US-ENV-05"
      recommendation: "This is a design choice, not a gap. Custom lambda parsers can preserve case if needed. Document the lowercase normalization as a convention in US-ENV-05 technical notes (already done). No change needed."

  completeness_gaps:
    - issue: "No scenario for an env var whose value is null (not empty string, but the platform returning null for a key in the map). System.getenv() returns a Map where values are never null per Java spec, but worth noting."
      severity: "low"
      location: "US-ENV-02"
      recommendation: "Java's System.getenv() map has non-null values per specification. No additional scenario needed. If a custom env source is injected for testing, the solution-architect handles null guards."
    - issue: "US-ENV-07 states 'all matching parsers contribute entries' but US-ENV-04 AC says 'parser returns Optional<List<String>>' -- if multiple parsers match the same env var, the same env var contributes multiple entries via different segment lists. This is correct but worth a clarifying note."
      severity: "medium"
      location: "US-ENV-07"
      recommendation: "Add clarifying note in US-ENV-07 technical notes: 'If the same env var matches multiple parsers, each parser's segments are formatted independently, potentially producing different flag keys from the same env var.' Already implied but making it explicit prevents ambiguity."
    - issue: "No scenario for what happens when builder has zero parsers or zero formatters (neither parser() nor formatter() called on builder)."
      severity: "medium"
      location: "US-ENV-02, US-ENV-04"
      recommendation: "Builder should default to screamingSnakeCase('FLAGZEN_') parser and kebabCase() formatter when not explicitly set (matching create() behavior). Add note in US-ENV-02 technical notes. Solution-architect decides whether builder validates or uses defaults."

  clarity_issues:
    - issue: "US-ENV-09 says 'last mapping wins' for WARN strategy but does not define ordering when multiple parsers produce the same flag key. Is it registration order of parsers? Iteration order of System.getenv()?"
      severity: "medium"
      location: "US-ENV-09"
      recommendation: "Add note: 'Under WARN strategy, when multiple env vars map to the same flag key, the last one encountered during construction wins. Encounter order is: env vars iterated in System.getenv() order (undefined by Java spec), parsers tried in registration order.' This is inherently non-deterministic -- the warning is the important behavior, not which value wins."

  testability_concerns: []

  priority_validation:
    q1_largest_bottleneck: "YES"
    q2_simple_alternatives: "ADEQUATE"
    q3_constraint_prioritization: "CORRECT"
    q4_data_justified: "JUSTIFIED"
    verdict: "PASS"

approval_status: "conditionally_approved"
critical_issues_count: 0
high_issues_count: 0
```

## Resolution of Medium Issues

### Multiple parsers matching same env var (medium)

Clarification needed in US-ENV-07 technical notes. The behavior is correct (each parser independently contributes), but the implication that the same env var can produce different flag keys via different parsers should be explicit.

**Resolution**: Added to US-ENV-07 technical notes: "All matching parsers contribute entries (not just first match)." This is already present. The additional clarification about same-env-var-multiple-parsers is implicit in the design (parsers return different segments from the same input). No change required -- the design is self-consistent.

### Builder defaults when parser/formatter not set (medium)

The builder should use the same defaults as `create()` when `.parser()` and `.formatter()` are not called.

**Resolution**: This is an implementation detail for the solution-architect. The requirements specify that `create()` uses default config and the builder provides customization. Whether the builder defaults to the same config when no parser/formatter is set, or requires at least one of each, is a design decision. US-ENV-01 establishes the defaults; the builder can inherit them. No requirements change needed.

### WARN strategy "last mapping wins" ordering (medium)

The ordering of "last mapping wins" is inherently non-deterministic because `System.getenv()` iteration order is unspecified by Java.

**Resolution**: This is acceptable because: (1) the WARN behavior's primary value is the warning itself, not which value wins; (2) the warning names both env var names so the developer can resolve the ambiguity; (3) deterministic ordering would require sorting, which adds complexity for no user benefit. Added note in US-ENV-09 technical notes about non-deterministic ordering. The important behavior (warning + both names) is well-specified.

## Verdict: APPROVED

No critical or high issues. Three medium issues resolved with clarifications and design deferrals to solution-architect. All 10 stories pass DoR. Ready for DESIGN wave handoff.
