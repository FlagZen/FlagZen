# Shared Artifacts Registry: flagzen-spring

## Artifacts

### flagzen-spring-version

- **Source of truth**: `gradle.properties` or Gradle version catalog
- **Consumers**: `build.gradle.kts` dependency declaration, auto-configuration metadata
- **Owner**: flagzen build system
- **Integration risk**: LOW -- standard Gradle version management
- **Validation**: Version in published POM matches declared version

### flag-provider-bean

- **Source of truth**: Spring `ApplicationContext` (single `FlagProvider` bean)
- **Consumers**: `FlagZenAutoConfiguration`, `FeatureDispatcher` bean, feature proxy beans
- **Owner**: Application developer (explicit `@Bean`) or auto-configuration (fallback)
- **Integration risk**: MEDIUM -- multiple providers cause ambiguity; missing provider requires fallback logic
- **Validation**: Exactly one `FlagProvider` resolvable at startup. If zero, fallback created with warning. If multiple, `@Primary` required.

### feature-dispatcher-bean

- **Source of truth**: `FlagZenAutoConfiguration` (creates `FeatureDispatcher` from `FlagProvider`)
- **Consumers**: Feature proxy bean registration, application code via `@Autowired FeatureDispatcher`
- **Owner**: `FlagZenAutoConfiguration`
- **Integration risk**: LOW -- single creation point, `@ConditionalOnMissingBean` guard
- **Validation**: Bean exists in context and uses the correct `FlagProvider`

### feature-metadata-registry

- **Source of truth**: `ServiceLoader<FeatureMetadata>` classpath scan
- **Consumers**: Feature proxy bean registration in auto-configuration
- **Owner**: Annotation processor (compile-time generated)
- **Integration risk**: HIGH -- if annotation processor not configured, no metadata exists, no beans registered. Developer gets confusing `NoSuchBeanDefinitionException` instead of clear FlagZen error.
- **Validation**: At startup, log count of discovered `FeatureMetadata` instances. If zero, log informational message.

### feature-proxy-bean

- **Source of truth**: `FeatureDispatcher.resolve(featureType)` called during bean registration
- **Consumers**: `@Autowired` injection points in application services
- **Owner**: Auto-configuration (registers bean definition per `FeatureMetadata`)
- **Integration risk**: MEDIUM -- proxy is singleton, must use same `FeatureDispatcher` that has the correct `FlagProvider`
- **Validation**: Bean type matches `@Feature` interface. Proxy dispatches to correct variant at runtime.

### auto-configuration-imports

- **Source of truth**: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- **Consumers**: Spring Boot auto-configuration discovery
- **Owner**: flagzen-spring module
- **Integration risk**: LOW -- standard Spring Boot mechanism. Wrong path = silent failure (no auto-config).
- **Validation**: File exists in published JAR. Contains FQCN of `FlagZenAutoConfiguration`.
