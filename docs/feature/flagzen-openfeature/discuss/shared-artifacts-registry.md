# Shared Artifacts Registry -- flagzen-openfeature

## Artifacts

### flag_key

- **Source of truth**: `@Feature` annotation `value` attribute on the user's interface
- **Consumers**: FlagZen generated proxy, `OpenFeatureFlagProvider.getString()`, OpenFeature `Client.getStringDetails()`
- **Owner**: Application developer (defines the key)
- **Integration risk**: LOW -- key is a passthrough string, no transformation
- **Validation**: Flag key passed to OpenFeature client unchanged; unit test asserts key identity

### targeting_key

- **Source of truth**: `com.flagzen.EvaluationContext.targetingKey()`
- **Consumers**: FlagZen `EvaluationContext`, OpenFeature `EvaluationContext` (via mapper)
- **Owner**: Application developer (sets targeting key at call site)
- **Integration risk**: LOW -- 1:1 string mapping
- **Validation**: Unit test asserts mapped OpenFeature context has identical targeting key

### context_attributes

- **Source of truth**: `com.flagzen.EvaluationContext.attributes()` (`Map<String, Object>`)
- **Consumers**: FlagZen `EvaluationContext`, OpenFeature `EvaluationContext` (as `Value` wrappers)
- **Owner**: Application developer
- **Integration risk**: MEDIUM -- type conversion from `Object` to OpenFeature `Value`. Unsupported types silently dropped with warning log.
- **Validation**: Unit tests for each supported type (String, Boolean, Integer, Long, Double, List, Map). Test for unsupported type producing warning.

### service_loader_registration

- **Source of truth**: `META-INF/services/com.flagzen.spi.FlagProvider` in flagzen-openfeature resources
- **Consumers**: `java.util.ServiceLoader` in FlagZen dispatcher setup
- **Owner**: flagzen-openfeature module
- **Integration risk**: HIGH -- wrong FQCN or missing file breaks auto-discovery silently
- **Validation**: Integration test: ServiceLoader discovers `OpenFeatureFlagProvider` from classpath

### module_artifact_id

- **Source of truth**: `flagzen-openfeature/build.gradle.kts` (group + artifactId)
- **Consumers**: Application developer's `build.gradle.kts`, Maven Central
- **Owner**: FlagZen project
- **Integration risk**: LOW -- standard Gradle convention
- **Validation**: Published artifact coordinates match documentation
