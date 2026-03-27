# Shared Artifacts Registry -- FlagZen

## Registry

### library_group_id

- **Source of truth**: Root `build.gradle.kts` (`group` property)
- **Value**: `com.flagzen`
- **Consumers**: README, build.gradle examples, Maven Central metadata, documentation site
- **Owner**: flagzen-core (root project)
- **Integration risk**: HIGH -- mismatch breaks all dependency declarations
- **Validation**: Maven Central coordinates must match root build file

### library_version

- **Source of truth**: `gradle.properties` (single version property)
- **Value**: `${flagzenVersion}` (e.g., `1.1.0`)
- **Consumers**: README, build.gradle examples, all submodule POMs, Maven Central, documentation
- **Owner**: Root project
- **Integration risk**: HIGH -- version mismatch between modules breaks resolution
- **Validation**: All submodules inherit version from root; single source

### core_module_coordinates

- **Source of truth**: `flagzen-core/build.gradle.kts`
- **Value**: `com.flagzen:flagzen-core`
- **Consumers**: build.gradle examples, annotationProcessor declarations, transitive dependency resolution
- **Owner**: flagzen-core module
- **Integration risk**: HIGH -- wrong coordinates = dependency not found
- **Validation**: Published artifact coordinates match build file

### test_module_coordinates

- **Source of truth**: `flagzen-test/build.gradle.kts`
- **Value**: `com.flagzen:flagzen-test`
- **Consumers**: build.gradle examples (testImplementation), documentation
- **Owner**: flagzen-test module
- **Integration risk**: HIGH
- **Validation**: Published artifact coordinates match build file

### spring_module_coordinates

- **Source of truth**: `flagzen-spring/build.gradle.kts`
- **Value**: `com.flagzen:flagzen-spring`
- **Consumers**: build.gradle examples (Spring users), Spring Boot auto-configuration
- **Owner**: flagzen-spring module
- **Integration risk**: HIGH
- **Validation**: Published artifact coordinates match build file

### feature_flag_key

- **Source of truth**: `@Feature` annotation `value` attribute
- **Value**: String (e.g., `"checkout-flow"`)
- **Consumers**: @Variant annotation processor validation, FlagProvider configuration, @PinFlag in tests, FeatureDispatcher.resolve(), runtime proxy dispatch, documentation examples
- **Owner**: Developer (defined per feature interface)
- **Integration risk**: HIGH -- key mismatch between @Feature and FlagProvider config causes silent failures or exceptions
- **Validation**: Annotation processor registers keys; runtime resolution validates against provider

### variant_value

- **Source of truth**: `@Variant` annotation `value` attribute
- **Value**: String (e.g., `"CLASSIC"`, `"STREAMLINED"`)
- **Consumers**: Compile-time enum validation, proxy dispatch switch/map, @PinFlag test setup, flag provider return values
- **Owner**: Developer (defined per variant class)
- **Integration risk**: HIGH -- variant value must match what the flag provider returns
- **Validation**: Compile-time validation against Variant enum (if present); runtime matching in proxy

### variant_enum_values

- **Source of truth**: Optional inner `Variant` enum on `@Feature` interface
- **Value**: Enum constant names (e.g., `CLASSIC, STREAMLINED, PREMIUM`)
- **Consumers**: Annotation processor (@Variant value validation), REQUIRED fallback completeness check, documentation
- **Owner**: Developer (defined on feature interface)
- **Integration risk**: MEDIUM -- enum is optional; when present, constrains all @Variant values
- **Validation**: Annotation processor validates @Variant values against enum constants

### dispatcher_instance

- **Source of truth**: `FlagZen.dispatcher()` factory or DI container bean
- **Value**: `FeatureDispatcher` instance
- **Consumers**: Application code, Spring auto-configuration, test context
- **Owner**: flagzen-core (factory), flagzen-spring (auto-configuration)
- **Integration risk**: MEDIUM -- must be configured with at least one FlagProvider
- **Validation**: Runtime check at first resolution; clear error if no provider

### flag_provider_config

- **Source of truth**: `FlagProvider` SPI implementation (e.g., EnvironmentVariableFlagProvider)
- **Value**: Implementation-specific configuration
- **Consumers**: Proxy runtime dispatch, evaluation context handling
- **Owner**: flagzen-env (or other provider modules)
- **Integration risk**: MEDIUM -- provider must be configured and return values matching variant strings
- **Validation**: SPI service loader discovery; runtime resolution attempt

### annotation_processor_metadata

- **Source of truth**: `META-INF/services/javax.annotation.processing.Processor`
- **Value**: Fully qualified class name of FlagZen annotation processor
- **Consumers**: Java compiler (javac), Gradle/Maven build tooling
- **Owner**: flagzen-core
- **Integration risk**: HIGH -- if missing, no compile-time validation or code generation occurs (silent failure)
- **Validation**: Build smoke test -- project with @Feature must trigger processor output

## Integration Checkpoints

### Checkpoint 1: Annotation Processor Discovery

- **What**: Adding flagzen-core to annotationProcessor config triggers the processor
- **Validates**: META-INF/services entry is correct, processor class is on processor path
- **Failure mode**: Silent -- no compile errors, but no generated proxies either
- **Detection**: Verify proxy class exists in generated sources after compilation

### Checkpoint 2: Feature-Variant Linkage

- **What**: @Variant classes are linked to @Feature interfaces by implements clause
- **Validates**: Type relationship + annotation metadata are consistent
- **Failure mode**: Compile error (processor validates this)
- **Detection**: Annotation processor output

### Checkpoint 3: Flag Key Consistency

- **What**: Flag key in @Feature matches key in FlagProvider configuration
- **Validates**: End-to-end key string consistency
- **Failure mode**: Runtime -- proxy cannot resolve variant (exception or noop depending on strategy)
- **Detection**: Runtime resolution; testable via @PinFlag

### Checkpoint 4: Variant Value Consistency

- **What**: Variant values in @Variant match values returned by FlagProvider
- **Validates**: End-to-end variant string consistency
- **Failure mode**: Runtime -- unmatched variant (exception or noop depending on strategy)
- **Detection**: Runtime resolution; testable via @PinFlag; compile-time validation against enum

### Checkpoint 5: Spring Auto-Configuration

- **What**: Adding flagzen-spring auto-registers FactoryBeans for @Feature interfaces
- **Validates**: Spring Boot conditional configuration, component scanning
- **Failure mode**: @Autowired field is null or Spring context fails to start
- **Detection**: Spring Boot integration test
