# Test Scenario Inventory -- flagzen-env

## Summary

| Category | Count |
| --- | --- |
| Walking skeleton scenarios | 2 |
| Focused scenarios (pending) | 31 |
| **Total** | **33** |
| Error/edge scenarios | 14 |
| **Error path ratio** | **42%** |

## Scenario-to-Story Traceability

### US-ENV-01: Zero-Config Default

| Scenario | File | Tags |
| --- | --- | --- |
| Developer resolves a flag from an environment variable with zero configuration | walking-skeleton.feature | @walking-skeleton @US-ENV-01 |
| Missing flag key returns no value | walking-skeleton.feature | @walking-skeleton @US-ENV-01 |
| Default configuration resolves multi-segment flag key | milestone-2-env-provider.feature | @pending @US-ENV-01 |
| Default configuration resolves single-segment flag key | milestone-2-env-provider.feature | @pending @US-ENV-01 |
| Non-matching environment variables are excluded from flag map | milestone-2-env-provider.feature | @pending @US-ENV-01 |

### US-ENV-02: Eager Loading with Immutable Map

| Scenario | File | Tags |
| --- | --- | --- |
| Developer resolves a flag from an environment variable with zero configuration | walking-skeleton.feature | @walking-skeleton @US-ENV-02 |
| Missing flag key returns no value | walking-skeleton.feature | @walking-skeleton @US-ENV-02 |
| Flag lookups are consistent after construction | milestone-2-env-provider.feature | @pending @US-ENV-02 |
| Empty environment variable value is preserved | milestone-2-env-provider.feature | @pending @US-ENV-02 |
| Context-aware lookup ignores evaluation context for static env vars | milestone-2-env-provider.feature | @pending @US-ENV-02 |
| Unparseable integer value returns no typed result but string is available | milestone-2-env-provider.feature | @pending @US-ENV-02 |
| Unparseable boolean value returns no typed result but string is available | milestone-2-env-provider.feature | @pending @US-ENV-02 |

### US-ENV-03: ServiceLoader Registration

| Scenario | File | Tags |
| --- | --- | --- |
| Provider is discoverable via service loading | milestone-2-env-provider.feature | @pending @US-ENV-03 |
| Auto-discovered provider resolves flags with default configuration | milestone-2-env-provider.feature | @pending @US-ENV-03 |

### US-ENV-04: Custom Parser Configuration

| Scenario | File | Tags |
| --- | --- | --- |
| Builder accepts a custom prefix parser | milestone-2-env-provider.feature | @pending @US-ENV-04 |
| Custom prefix parser excludes non-matching environment variables | milestone-2-env-provider.feature | @pending @US-ENV-04 |
| Builder accepts a custom lambda parser | milestone-2-env-provider.feature | @pending @US-ENV-04 |
| Builder accepts a custom formatter | milestone-2-env-provider.feature | @pending @US-ENV-04 |

### US-ENV-05: Built-in Parsers

| Scenario | File | Tags |
| --- | --- | --- |
| Developer resolves a flag from an environment variable with zero configuration | walking-skeleton.feature | @walking-skeleton @US-ENV-05 |
| Screaming snake case parser with prefix extracts segments | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Screaming snake case parser rejects names without matching prefix | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Screaming snake case parser without prefix parses any screaming snake name | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Screaming snake case parser handles single-segment name | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Camel case parser with prefix extracts segments | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Camel case parser rejects names without matching prefix | milestone-1-key-mapping.feature | @pending @US-ENV-05 |
| Camel case parser without prefix parses bare camel case name | milestone-1-key-mapping.feature | @pending @US-ENV-05 |

### US-ENV-06: Built-in Formatters

| Scenario | File | Tags |
| --- | --- | --- |
| Developer resolves a flag from an environment variable with zero configuration | walking-skeleton.feature | @walking-skeleton @US-ENV-06 |
| Built-in formatter produces correct flag key (6 examples) | milestone-1-key-mapping.feature | @pending @US-ENV-06 |
| Formatter handles single segment without delimiter | milestone-1-key-mapping.feature | @pending @US-ENV-06 |
| Custom lambda formatter applies custom delimiter | milestone-1-key-mapping.feature | @pending @US-ENV-06 |

### US-ENV-07: Multiple Parsers

| Scenario | File | Tags |
| --- | --- | --- |
| Multiple parsers contribute different flags from different conventions | milestone-3-conflict-strategy.feature | @pending @US-ENV-07 |
| Multiple parsers with no overlapping flags produce no conflict | milestone-3-conflict-strategy.feature | @pending @US-ENV-07 |
| Warn strategy logs conflict and continues operating | milestone-3-conflict-strategy.feature | @pending @US-ENV-07 @US-ENV-09 |
| Error strategy rejects construction when conflict is detected | milestone-3-conflict-strategy.feature | @pending @US-ENV-07 @US-ENV-09 |

### US-ENV-08: Multiple Formatters

| Scenario | File | Tags |
| --- | --- | --- |
| Multiple formatters produce multiple flag keys from one environment variable | milestone-3-conflict-strategy.feature | @pending @US-ENV-08 |
| Single-segment key resolves identically from both formatters | milestone-3-conflict-strategy.feature | @pending @US-ENV-08 |

### US-ENV-09: ConflictStrategy

| Scenario | File | Tags |
| --- | --- | --- |
| Single parser and single formatter default to warn on conflict | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 |
| Multiple parsers and single formatter default to warn on conflict | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 |
| Single parser and multiple formatters default to warn on conflict | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 |
| Multiple parsers and multiple formatters default to error on conflict | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 |
| Multiple parsers and multiple formatters can be overridden to warn | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 |
| Warn strategy logs conflict and continues operating | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 @US-ENV-07 |
| Error strategy rejects construction when conflict is detected | milestone-3-conflict-strategy.feature | @pending @US-ENV-09 @US-ENV-07 |

### US-ENV-10: First-Access Conflict Warning

| Scenario | File | Tags |
| --- | --- | --- |
| First access of a conflicted flag key produces a warning | milestone-3-conflict-strategy.feature | @pending @US-ENV-10 |
| Subsequent access of the same conflicted flag key produces no warning | milestone-3-conflict-strategy.feature | @pending @US-ENV-10 |
| Non-conflicted flag key produces no warning on access | milestone-3-conflict-strategy.feature | @pending @US-ENV-10 |

## Error/Edge Case Inventory (14 scenarios, 42%)

1. Missing flag key returns no value (walking skeleton)
2. Screaming snake case parser rejects names without matching prefix
3. Camel case parser rejects names without matching prefix
4. Non-matching environment variables are excluded from flag map
5. Empty environment variable value is preserved
6. Unparseable integer value returns no typed result but string is available
7. Unparseable boolean value returns no typed result but string is available
8. Custom prefix parser excludes non-matching environment variables
9. Multiple parsers with no overlapping flags produce no conflict
10. Error strategy rejects construction when conflict is detected
11. Subsequent access of the same conflicted flag key produces no warning
12. Non-conflicted flag key produces no warning on access
13. Formatter handles single segment without delimiter
14. Single-segment key resolves identically from both formatters
