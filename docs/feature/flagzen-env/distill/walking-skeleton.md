# Walking Skeleton Rationale -- flagzen-env

## Skeleton Selection

The walking skeleton covers the thinnest possible end-to-end slice:

**Environment variable** `FLAGZEN_CHECKOUT_FLOW=CLASSIC` **->** default parser (screamingSnakeCase with FLAGZEN_ prefix) **->** default formatter (kebabCase) **->** immutable flag map **->** `getString("checkout-flow")` returns `"CLASSIC"`.

## Why This Slice

This skeleton exercises every component in the pipeline with zero configuration:

1. **FlagKeyParsers.screamingSnakeCase("FLAGZEN_")** -- parses the env var name
2. **FlagKeyFormats.kebabCase()** -- formats segments into flag key
3. **EnvironmentVariableFlagProvider.create()** -- zero-config factory
4. **Eager loading pipeline** -- reads env vars, runs parse/format, freezes map
5. **FlagProvider.getString()** -- returns value from immutable map

A non-technical stakeholder can confirm: "Yes, setting an environment variable and reading it as a flag is the core value."

## Scenarios

| Scenario | Stories | Purpose |
| --- | --- | --- |
| Developer resolves a flag from an environment variable with zero configuration | US-ENV-01, US-ENV-02, US-ENV-05, US-ENV-06 | Happy path: full pipeline, observable value |
| Missing flag key returns no value | US-ENV-01, US-ENV-02 | Error path: absent env var handled gracefully |

## Walking Skeleton Litmus Test

1. **Title describes user goal**: "Developer resolves a flag from an environment variable with zero configuration" -- yes, user goal.
2. **Given/When describe user actions**: setting env var, creating provider, looking up flag -- yes, user actions.
3. **Then describes user observation**: "the flag value is CLASSIC" -- yes, observable outcome.
4. **Non-technical stakeholder can confirm**: "Can a developer set an env var and read it as a flag? Yes." -- passes.

## Not Walking Skeleton (Rationale for Exclusion)

- ServiceLoader discovery (US-ENV-03) -- auto-discovery is a DX convenience, not core value delivery.
- Custom parsers/formatters (US-ENV-04, US-ENV-07, US-ENV-08) -- customization extends the default, not the thinnest slice.
- Conflict handling (US-ENV-09, US-ENV-10) -- conflict is a secondary concern for multi-convention setups.
