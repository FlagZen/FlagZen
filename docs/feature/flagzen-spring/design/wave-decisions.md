# Wave Decisions: flagzen-spring DESIGN

## Decision Summary

|  #  |             Decision              |                               Choice                               |                                                                            Rationale                                                                            |
| --- | --------------------------------- | ------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Development paradigm              | OOP (Java 17+)                                                     | Established project convention                                                                                                                                  |
| 2   | Architectural style               | Ports-and-adapters (adapter module)                                | flagzen-spring is a driving adapter wiring flagzen-core into Spring DI                                                                                          |
| 3   | Proxy bean registration mechanism | `ImportBeanDefinitionRegistrar`                                    | Standard Spring mechanism for dynamic bean registration; cleaner lifecycle than `BeanDefinitionRegistryPostProcessor`, simpler than `FactoryBean` (see ADR-019) |
| 4   | FlagProvider detection            | Standard Spring DI (constructor/method parameter injection)        | No custom resolution logic; `@Primary`, `@Qualifier`, `@Profile` work out of the box                                                                            |
| 5   | Fallback provider                 | `InMemoryFlagProvider` with `@ConditionalOnMissingBean` + WARN log | App always starts; clear signal that production provider is missing                                                                                             |
| 6   | Spring Boot version               | 3.x only (`AutoConfiguration.imports`, not `spring.factories`)     | DISCUSS wave decision; aligns with Jakarta namespace, current LTS                                                                                               |
| 7   | Configuration properties          | None for v1.1.0                                                    | Convention-based auto-configuration; no `flagzen.*` properties needed                                                                                           |
| 8   | Feature proxy bean scope          | Singleton, lazy-initialized                                        | Matches `FeatureDispatcher` proxy cache; lazy avoids startup ordering issues                                                                                    |
| 9   | Package structure                 | Single package `com.flagzen.spring` (2 classes)                    | Module too small for sub-packages; flat structure is clearest                                                                                                   |
| 10  | Variant lifecycle                 | Supplier-based (POJO)                                              | DISCUSS wave Decision 5; Spring-managed variants deferred to future release                                                                                     |

## Artifacts Produced

|       Artifact       |                             Path                             |
| -------------------- | ------------------------------------------------------------ |
| Architecture design  | `docs/feature/flagzen-spring/design/architecture-design.md`  |
| Component boundaries | `docs/feature/flagzen-spring/design/component-boundaries.md` |
| Data models          | `docs/feature/flagzen-spring/design/data-models.md`          |
| Wave decisions       | `docs/feature/flagzen-spring/design/wave-decisions.md`       |
| ADR-019              | `docs/adrs/ADR-019-proxy-bean-registration-strategy.md`      |

## Handoff Notes for Acceptance Designer (DISTILL wave)

- All user stories from DISCUSS have behavioral acceptance criteria -- no implementation coupling
- Key test scenarios: auto-config happy path, `@ConditionalOnMissingBean` back-off, `InMemoryFlagProvider` fallback, zero-metadata graceful degradation, dynamic dispatch after injection
- Use `ApplicationContextRunner` for lightweight auto-configuration tests (Spring Boot test utility)
- Integration tests need a test `@Feature` interface with generated metadata on the test classpath

## Handoff Notes for Platform Architect (DEVOPS wave)

- New Gradle submodule `flagzen-spring` to add to `settings.gradle.kts`: `include("flagzen-spring")`
- Dependencies: `api("com.flagzen:flagzen-core")`, `implementation("org.springframework.boot:spring-boot-autoconfigure")`
- Test dependency: `testImplementation("org.springframework.boot:spring-boot-starter-test")`
- CI: Run tests with Spring Boot 3.2.x and 3.3.x to verify compatibility
- External integration: Spring Boot Autoconfigure API -- version matrix testing recommended (not Pact; this is a framework API, not a service)
