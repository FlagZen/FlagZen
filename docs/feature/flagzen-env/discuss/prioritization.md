# Prioritization: flagzen-env

## Release Priority

| Priority | Release | Target Outcome | KPI | Rationale |
| --- | --- | --- | --- | --- |
| 1 | R1: Sensible Defaults (WS) | Env vars work as flag source end-to-end, zero config | Developers resolve flags from env vars, 0 config lines | Core value proposition; everything else builds on this |
| 2 | R2: Custom Configuration | Any parser/prefix supported via builder | Custom prefix works without forking provider | Enables adoption by teams with existing conventions |
| 3 | R3: Multi-Convention Support | One provider handles mixed conventions with conflict control | Eliminates need for multiple provider instances | High-value for migrations and multi-team services |

## Backlog Suggestions

| Story | Release | Priority | Outcome Link | Dependencies |
| --- | --- | --- | --- | --- |
| US-ENV-05 | R1 | P1 | Built-in parsers decompose env var names into segments | None |
| US-ENV-06 | R1 | P1 | Built-in formatters produce flag keys from segments | None |
| US-ENV-02 | R1 | P1 | Eager loading with immutable map for fast resolution | US-ENV-05, US-ENV-06 |
| US-ENV-01 | R1 | P1 | Zero-config defaults work out of the box | US-ENV-02, US-ENV-05, US-ENV-06 |
| US-ENV-03 | R1 | P1 | ServiceLoader auto-discovery with default config | US-ENV-02 |
| US-ENV-04 | R2 | P2 | Custom prefix and lambda parser via builder | US-ENV-02, US-ENV-05 |
| US-ENV-09 | R3 | P3 | Conflict detection with configurable strategy | US-ENV-02 |
| US-ENV-07 | R3 | P3 | Multiple parsers for legacy migration | US-ENV-04, US-ENV-09 |
| US-ENV-08 | R3 | P3 | Multiple formatters for multi-convention codebases | US-ENV-06, US-ENV-09 |
| US-ENV-10 | R3 | P3 | Conflict warning on first access | US-ENV-09 |

> **Note**: R1 stories form one atomic delivery -- the parse-format pipeline needs parsers (US-ENV-05), formatters (US-ENV-06), the provider (US-ENV-02), defaults (US-ENV-01), and ServiceLoader (US-ENV-03) to be useful. R2 (US-ENV-04) is independently deliverable. R3 stories are tightly coupled (conflict handling + multi-parser + multi-formatter + access warning).
>
> **Module split**: US-ENV-05, US-ENV-06, and US-ENV-09 target `flagzen-key-mapping` (reusable key-mapping infrastructure). All other stories target `flagzen-env`. The `flagzen-key-mapping` module ships as part of R1 (parsers + formatters) and R3 (ConflictStrategy).

## MoSCoW

| Category | Stories | Rationale |
| --- | --- | --- |
| Must Have | US-ENV-01, US-ENV-02, US-ENV-03, US-ENV-05, US-ENV-06 | Core module -- required for the env var provider to function |
| Should Have | US-ENV-04 | High value for adoption in teams with existing conventions |
| Could Have | US-ENV-07, US-ENV-08, US-ENV-09, US-ENV-10 | Valuable for multi-convention and migration scenarios; workaround exists (multiple providers) |
| Won't Have (this release) | Env var watching/reload, file-based env loading, case-sensitivity options | Simplicity first; add complexity only if demand emerges |

## Value/Effort Matrix

| | Low Effort | High Effort |
| --- | --- | --- |
| **High Value** | US-ENV-01, US-ENV-02, US-ENV-03 | US-ENV-05 (2 parser types with prefix variants), US-ENV-06 (6 formatters) |
| **Low Value** | US-ENV-04 (builder parameter) | US-ENV-07, US-ENV-08, US-ENV-09, US-ENV-10 (conflict handling subsystem) |

US-ENV-05 and US-ENV-06 are placed at High Effort because they require multiple parsing/formatting algorithms. US-ENV-04 is low effort (adding a builder parameter) but lower independent value since it requires R1 to exist. R3 stories (US-ENV-07 through US-ENV-10) form a subsystem that is high effort collectively but addresses a niche (multi-convention codebases).
