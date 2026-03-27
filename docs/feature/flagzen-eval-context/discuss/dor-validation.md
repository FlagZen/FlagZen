# Definition of Ready Validation: Evaluation Context (flagzen-eval-context)

## US-EC-01: EvaluationContext Model

|        DoR Item         | Status |                                                               Evidence/Issue                                                                |
| ----------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "Kenji needs to resolve flags differently per user, but FlagZen has no model to carry targeting information" -- domain language, clear pain |
| User/persona identified | PASS   | Kenji Tanaka, senior Java dev, SaaS company, uses FlagZen M0, needs per-user/tenant targeting                                               |
| 3+ domain examples      | PASS   | 3 examples: A/B test targeting (enterprise plan), anonymous context (no targeting key), empty context (key only)                            |
| UAT scenarios (3-7)     | PASS   | 4 scenarios: build with key+attrs, no targeting key, toString, equality. Immutability verified by unit test (architectural constraint).      |
| AC derived from UAT     | PASS   | 6 ACs matching scenarios (no-setter design, builder, nullable key, empty map, toString/equals/hashCode, zero reflection)                    |
| Right-sized             | PASS   | 1-2 days effort, 5 scenarios, single model class with builder                                                                               |
| Technical notes         | PASS   | Record consideration, Object attribute values, thread safety, no validation on values                                                       |
| Dependencies tracked    | PASS   | None (foundation story)                                                                                                                     |
| Outcome KPIs defined    | PASS   | API ergonomics -- 100% fluent chain construction                                                                                            |

### DoR Status: PASSED

---

## US-EC-02: Explicit Context on FeatureDispatcher.resolve()

|        DoR Item         | Status |                                      Evidence/Issue                                       |
| ----------------------- | ------ | ----------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "No way to pass EvaluationContext to resolve(), forcing all users to get same flag value" |
| User/persona identified | PASS   | Java developer with EvaluationContext, wants per-request resolution                       |
| 3+ domain examples      | PASS   | 3 examples: enterprise VIP, free-tier, existing code unchanged                            |
| UAT scenarios (3-7)     | PASS   | 3 scenarios: context forwarded, backward compat, null context                             |
| AC derived from UAT     | PASS   | 4 ACs: overload added, backward compat, null handling, context forwarded                  |
| Right-sized             | PASS   | 1 day effort, 3 scenarios, one method overload                                            |
| Technical notes         | PASS   | Interface default method for backward compat, DefaultFeatureDispatcher implementation     |
| Dependencies tracked    | PASS   | US-EC-01, US-EC-03                                                                        |
| Outcome KPIs defined    | PASS   | Zero additional lines vs contextless resolve                                              |

**DoR Status: PASSED**

---

## US-EC-03: FlagProvider Context-Aware Overload

|        DoR Item         | Status |                                              Evidence/Issue                                               |
| ----------------------- | ------ | --------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "FlagProvider only receives getString(key), no way to receive context; existing providers must not break" |
| User/persona identified | PASS   | FlagProvider implementor, has existing provider code                                                      |
| 3+ domain examples      | PASS   | 3 examples: LaunchDarkly adapter, InMemoryFlagProvider unchanged, custom partial context                  |
| UAT scenarios (3-7)     | PASS   | 3 scenarios: default delegates, provider override, existing compile                                       |
| AC derived from UAT     | PASS   | 4 ACs: default method, delegation, existing compile, override support                                     |
| Right-sized             | PASS   | 0.5 day effort, 3 scenarios, one default method                                                           |
| Technical notes         | PASS   | Default method, SPI change documentation, backward compat                                                 |
| Dependencies tracked    | PASS   | US-EC-01                                                                                                  |
| Outcome KPIs defined    | PASS   | Zero code changes for existing providers                                                                  |

**DoR Status: PASSED**

---

## US-EC-04: Generated Proxy Passes Context to FlagProvider

|        DoR Item         | Status |                                                       Evidence/Issue                                                        |
| ----------------------- | ------ | --------------------------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "Generated proxy calls getString(key) without context, context is lost between dispatcher and flag lookup"                  |
| User/persona identified | PASS   | Java developer using polymorphic dispatch with context                                                                      |
| 3+ domain examples      | PASS   | 3 examples: proxy with context, proxy without context, proxy with context-unaware provider                                  |
| UAT scenarios (3-7)     | PASS   | 3 scenarios: proxy forwards context, works without context, regeneration on upgrade                                         |
| AC derived from UAT     | PASS   | 5 ACs: proxy accepts context, getString(key, context) call, getString(key) fallback, zero reflection, no annotation changes |
| Right-sized             | PASS   | 2-3 days effort, 3 scenarios, annotation processor code generation change                                                   |
| Technical notes         | PASS   | ProxyGenerator modification, context passing mechanism, zero reflection constraint                                          |
| Dependencies tracked    | PASS   | US-EC-02, US-EC-03                                                                                                          |
| Outcome KPIs defined    | PASS   | Zero manual proxy code, annotation processor handles everything                                                             |

**DoR Status: PASSED**

---

## US-EC-05: Block-Scoped Context via FlagContext.run()

|        DoR Item         | Status |                                           Evidence/Issue                                           |
| ----------------------- | ------ | -------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "Threading context through every method in call stack is impractical for multiple resolve() calls" |
| User/persona identified | PASS   | Java developer with request-handling code, multiple resolve calls                                  |
| 3+ domain examples      | PASS   | 3 examples: Maria Santos request handler, Hiroshi Yamamoto nested scopes, Supplier variant         |
| UAT scenarios (3-7)     | PASS   | 5 scenarios: scoped applies, cleanup, nesting, supplier, exception cleanup                         |
| AC derived from UAT     | PASS   | 5 ACs: Runnable scope, Supplier scope, cleanup, nesting, thread safety                             |
| Right-sized             | PASS   | 2 days effort, 5 scenarios, FlagContext class with ThreadLocal                                     |
| Technical notes         | PASS   | ThreadLocal initial impl, ScopedValue deferred to US-EC-08, final class with static methods        |
| Dependencies tracked    | PASS   | US-EC-01                                                                                           |
| Outcome KPIs defined    | PASS   | Eliminates N parameter-passing changes                                                             |

**DoR Status: PASSED**

---

## US-EC-06: ContextAccessor SPI

|        DoR Item         | Status |                                                  Evidence/Issue                                                  |
| ----------------------- | ------ | ---------------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "Needs extension point for framework-specific context sources without explicit passing"                          |
| User/persona identified | PASS   | Framework adapter author, prepares for M6 reactive modules                                                       |
| 3+ domain examples      | PASS   | 3 examples: Reactor accessor (M6 preview), Servlet accessor, no accessor registered                              |
| UAT scenarios (3-7)     | PASS   | 4 scenarios: accessor provides context, priority ordering, empty skipped, no accessor                            |
| AC derived from UAT     | PASS   | 6 ACs: interface contract, ServiceLoader, priority sorting, first non-empty wins, empty skip, no accessor = skip |
| Right-sized             | PASS   | 1-2 days effort, 4 scenarios, SPI interface + ServiceLoader discovery                                            |
| Technical notes         | PASS   | SPI only (no implementations), ServiceLoader caching, priority conventions                                       |
| Dependencies tracked    | PASS   | US-EC-01                                                                                                         |
| Outcome KPIs defined    | PASS   | One interface + one ServiceLoader file to implement                                                              |

**DoR Status: PASSED**

---

## US-EC-07: Context Resolution Order

|        DoR Item         | Status |                                                       Evidence/Issue                                                       |
| ----------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "Multiple context sources, needs deterministic and intuitive resolution order"                                             |
| User/persona identified | PASS   | Java developer with multiple context sources active                                                                        |
| 3+ domain examples      | PASS   | 3 examples: Carlos Mendez (all sources, explicit wins), Priya Sharma (accessor wins), Ahmed Hassan (no context, M0 compat) |
| UAT scenarios (3-7)     | PASS   | 5 scenarios: explicit wins, accessor wins, scoped wins, default last resort, no context M0 compat                          |
| AC derived from UAT     | PASS   | 5 ACs: resolution order, Javadoc, independently testable, M0 compat, not configurable                                      |
| Right-sized             | PASS   | 1-2 days effort, 5 scenarios, resolution logic in DefaultFeatureDispatcher                                                 |
| Technical notes         | PASS   | Hardcoded order, accessor before scoped rationale, implementation location                                                 |
| Dependencies tracked    | PASS   | US-EC-02, US-EC-05, US-EC-06                                                                                               |
| Outcome KPIs defined    | PASS   | 100% deterministic, documented, tested with all permutations                                                               |

**DoR Status: PASSED**

---

## US-EC-08: ScopedValue Carrier for FlagContext.run() (Java 21+)

|        DoR Item         | Status |                                                 Evidence/Issue                                                 |
| ----------------------- | ------ | -------------------------------------------------------------------------------------------------------------- |
| Problem statement clear | PASS   | "ThreadLocal works but ScopedValue is recommended for virtual thread environments on Java 21+"                 |
| User/persona identified | PASS   | Java developer on Java 21+, virtual threads, optimal thread safety                                             |
| 3+ domain examples      | PASS   | 3 examples: Elena Rossi (Java 21 ScopedValue), Tomas Bergstrom (Java 17 ThreadLocal), mixed environment        |
| UAT scenarios (3-7)     | PASS   | 3 scenarios: ScopedValue on 21+, ThreadLocal on 17, identical API                                              |
| AC derived from UAT     | PASS   | 5 ACs: ScopedValue on 21+, ThreadLocal fallback, class-loading detection, no API difference, both thread types |
| Right-sized             | PASS   | 1-2 days effort, 3 scenarios, runtime detection + multi-release or conditional loading                         |
| Technical notes         | PASS   | ScopedValue preview status, multi-release JAR consideration, R2 story                                          |
| Dependencies tracked    | PASS   | US-EC-05                                                                                                       |
| Outcome KPIs defined    | PASS   | Zero virtual thread pinning from FlagContext operations                                                        |

**DoR Status: PASSED**

---

## Summary

|  Story   | DoR Status | Items Passed |
| -------- | ---------- | ------------ |
| US-EC-01 | PASSED     | 9/9          |
| US-EC-02 | PASSED     | 9/9          |
| US-EC-03 | PASSED     | 9/9          |
| US-EC-04 | PASSED     | 9/9          |
| US-EC-05 | PASSED     | 9/9          |
| US-EC-06 | PASSED     | 9/9          |
| US-EC-07 | PASSED     | 9/9          |
| US-EC-08 | PASSED     | 9/9          |

All 8 stories pass Definition of Ready. Ready for handoff to DESIGN wave.
