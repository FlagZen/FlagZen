# Wave Decisions: flagzen-spring (M4)

## Decision 1: Feature Type

**Cross-cutting** -- spans Spring DI layer + FlagZen core SPI discovery.

## Decision 2: Walking Skeleton

**Yes** -- thinnest slice: Spring Boot app auto-discovers `FlagProvider` bean, creates `FeatureDispatcher` bean, injects `@Feature` proxy via `@Autowired`.

## Decision 3: UX Research Depth

**Lightweight** -- standard Spring Boot starter pattern. Developers expect `@Autowired` injection. No novel UX to discover.

## Decision 4: JTBD

**Skipped** -- motivations clear. Spring developers expect DI-managed feature proxies. No competing jobs.

## Decision 5: Variant Instance Lifecycle

**Option A (Supplier-based)** for v1.1.0. Variant instances created via `Supplier::new` from `FeatureMetadata`. Variants are plain POJOs, no Spring DI inside them. This matches the existing zero-reflection contract.

**Option B (Spring-managed)** deferred to future release. Would allow `@Autowired` inside `@Variant` classes but requires changing how metadata creates instances.

## Decision 6: Spring Boot Version Target

Spring Boot 3.x (Spring Framework 6.x, Jakarta namespace). Uses `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (not legacy `spring.factories`).
