# Technology Stack -- flagzen-providers

## flagzen-launchdarkly

| Component | Technology | Version | License | Rationale |
| --- | --- | --- | --- | --- |
| Language | Java | 17+ | N/A | Project standard |
| Build | Gradle (Kotlin DSL) | Project version | Apache 2.0 | Project standard |
| Core dependency | flagzen-core | Same version | Apache 2.0 | SPI contract |
| External SDK | com.launchdarkly:launchdarkly-java-server-sdk | 7.x (latest stable) | Apache 2.0 | Only LaunchDarkly server SDK for Java |
| Logging | java.util.logging (JUL) | JDK | N/A | Consistent with other adapters; zero added deps |

### Why `com.launchdarkly:launchdarkly-java-server-sdk`

This is the only official SDK for LaunchDarkly server-side Java evaluation. There is no alternative. The SDK is maintained by LaunchDarkly Inc., released under Apache 2.0, with active development and regular releases.

**Version 7.x rationale**: v7 introduced `LDContext` (replacing `LDUser`), which supports multi-context evaluation. v6 is in maintenance mode. v7 is the current recommended version per LaunchDarkly documentation.

### Transitive Dependencies

The LaunchDarkly SDK brings significant transitive dependencies (OkHttp, Gson, etc.). These are `implementation` scope in `flagzen-launchdarkly`, so they do not leak to consumers. However, consumers who already use the LaunchDarkly SDK (likely, since they are choosing this adapter) already have these on their classpath.

---

## flagzen-togglz

| Component | Technology | Version | License | Rationale |
| --- | --- | --- | --- | --- |
| Language | Java | 17+ | N/A | Project standard |
| Build | Gradle (Kotlin DSL) | Project version | Apache 2.0 | Project standard |
| Core dependency | flagzen-core | Same version | Apache 2.0 | SPI contract |
| External SDK | org.togglz:togglz-core | 4.x (latest stable) | Apache 2.0 | Core Togglz API; no alternative |
| Logging | java.util.logging (JUL) | JDK | N/A | Consistent with other adapters; zero added deps |

### Why `org.togglz:togglz-core`

This is the core module of the Togglz feature toggle framework. Only `togglz-core` is needed (not `togglz-spring-boot-starter` or `togglz-servlet`). The adapter depends on `FeatureManager` and `FeatureState` interfaces, which are in the core module.

**Version 4.x rationale**: v4 is the current stable release. It supports Java 17+ and uses modern APIs. v3 had a minimum of Java 8 and lacked some API improvements.

### Transitive Dependencies

`togglz-core` has minimal transitive dependencies (SLF4J API only). The dependency footprint is small compared to LaunchDarkly.

---

## Build Configuration

### flagzen-launchdarkly/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    implementation("com.launchdarkly:launchdarkly-java-server-sdk:7.x.x")
}
```

### flagzen-togglz/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":flagzen-core"))
    implementation("org.togglz:togglz-core:4.x.x")
}
```

### settings.gradle.kts additions

```kotlin
include("flagzen-launchdarkly")
include("flagzen-togglz")
```

Both modules follow the exact same Gradle structure as `flagzen-openfeature`.
