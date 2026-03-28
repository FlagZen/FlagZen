# Shared Artifacts Registry -- flagzen-env

## Artifacts

### flag_key_parser

- **Source of truth**: `FlagKeyParser` SAM interface + `FlagKeyParsers` companion class
- **Consumers**:
  - EnvironmentVariableFlagProvider construction pipeline
  - Builder `.parser()` method
  - Documentation and examples
  - Gherkin scenarios
  - Other providers (file-based, vault) that need key parsing
- **Owner**: flagzen-key-mapping module
- **Integration risk**: HIGH -- parser behavior determines which env vars enter the flag map
- **Validation**: Parameterized unit tests asserting each parser with multiple input formats

### flag_key_format

- **Source of truth**: `FlagKeyFormat` SAM interface + `FlagKeyFormats` companion class
- **Consumers**:
  - EnvironmentVariableFlagProvider construction pipeline
  - Builder `.formatter()` method
  - getString() key lookups (flag keys must match formatter output)
  - Documentation and examples
  - Other providers that need key formatting
- **Owner**: flagzen-key-mapping module
- **Integration risk**: HIGH -- formatter output determines the flag keys used in getString() calls
- **Validation**: Parameterized unit tests asserting each formatter with standard segment inputs

### flagzen_key_mapping_artifact

- **Source of truth**: `flagzen-key-mapping/build.gradle` (group, name, version)
- **Consumers**:
  - flagzen-env (transitive dependency)
  - Other provider modules that need key parsing/formatting
  - Developers who want key mapping without any specific provider
- **Owner**: flagzen-key-mapping module
- **Integration risk**: LOW -- standard Gradle coordinates, versioned with parent
- **Validation**: Published artifact matches declared coordinates; flagzen-env declares it as dependency

### flagzen_env_artifact

- **Source of truth**: `flagzen-env/build.gradle` (group, name, version)
- **Consumers**:
  - Developer's build.gradle dependency declaration
  - Maven Central publication
  - Documentation snippets
- **Owner**: flagzen-env module
- **Integration risk**: LOW -- standard Gradle coordinates, versioned with parent
- **Validation**: Published artifact matches declared coordinates

### serviceloader_registration

- **Source of truth**: `flagzen-env/src/main/resources/META-INF/services/com.flagzen.spi.FlagProvider`
- **Consumers**:
  - `FlagZen.create()` factory
  - `ServiceLoader.load(FlagProvider.class)` at runtime
- **Owner**: flagzen-env module
- **Integration risk**: HIGH -- wrong FQCN or missing file means silent discovery failure
- **Validation**: Integration test confirming ServiceLoader discovers the provider

### builder_api

- **Source of truth**: `EnvironmentVariableFlagProvider.builder()` method chain
- **Consumers**:
  - Explicit construction in application code
  - Test fixtures
  - Documentation examples
- **Owner**: flagzen-env module
- **Integration risk**: MEDIUM -- builder API shape affects all custom configurations
- **Validation**: Integration tests with various builder configurations

### conflict_strategy

- **Source of truth**: `ConflictStrategy` enum (WARN, ERROR)
- **Consumers**:
  - Builder `.onConflict()` method
  - Construction-time conflict detection logic
  - First-access warning logging (US-ENV-10)
  - Documentation
  - Other providers with multi-source conflict scenarios
- **Owner**: flagzen-key-mapping module
- **Integration risk**: HIGH -- wrong default strategy for parser/formatter cardinality could cause silent data loss or unexpected failures
- **Validation**: Tests verifying default strategy per cardinality combination + explicit override

### immutable_flag_map

- **Source of truth**: Internal `Map<String, String>` built at construction time
- **Consumers**:
  - `getString(key)` lookups
  - Typed resolution via FlagProvider default methods
  - Conflict tracking set (for first-access warnings)
- **Owner**: flagzen-env module
- **Integration risk**: HIGH -- map correctness depends on parser + formatter pipeline
- **Validation**: Integration tests verifying end-to-end: env var -> parser -> formatter -> map -> getString()

### parse_format_pipeline

- **Source of truth**: Construction-time pipeline in `EnvironmentVariableFlagProvider`
- **Consumers**:
  - All flag resolution calls (indirectly, via immutable map)
  - Documentation describing the pipeline
  - Gherkin scenarios
- **Owner**: flagzen-env module
- **Integration risk**: HIGH -- pipeline order (parse env var name -> segments -> format flag key) must be consistent everywhere
- **Validation**: Integration tests verifying pipeline end-to-end

## Integration Checkpoints

| Checkpoint | Validates | Stories |
| --- | --- | --- |
| Parse-format pipeline determinism | Same env var + same config always produces same flag key | US-ENV-01, US-ENV-05, US-ENV-06 |
| Eager loading correctness | All matching env vars loaded at construction, immutable map complete | US-ENV-02 |
| ServiceLoader discovery | Provider found without explicit configuration (default config) | US-ENV-03 |
| FlagProvider contract compliance | All FlagProvider methods work correctly (getString, typed defaults) | US-ENV-02 |
| Transitive dependency | flagzen-env depends on flagzen-core, user does not need to declare both | US-ENV-02, US-ENV-03 |
| Custom parser configuration | Builder accepts custom parsers, prefix is per-parser | US-ENV-04 |
| Built-in parser correctness | screamingSnakeCase and camelCase parsers produce correct segments | US-ENV-05 |
| Built-in formatter correctness | All 6 formatters produce correct flag keys from segments | US-ENV-06 |
| Multiple parser contribution | Each parser independently contributes entries to the flag map | US-ENV-07 |
| Multiple formatter expansion | Each formatter produces flag key entries for each parsed env var | US-ENV-08 |
| Conflict strategy defaults | Default strategy matches parser/formatter cardinality rules | US-ENV-09 |
| First-access conflict warning | Conflicted key logs warning on first getString() call only | US-ENV-10 |
| Pipeline consistency | Parser -> segments -> formatter -> flag key is consistent across all configurations | US-ENV-01 through US-ENV-10 |
