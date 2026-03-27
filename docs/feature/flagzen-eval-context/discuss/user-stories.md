<!-- markdownlint-disable MD024 -->

# User Stories: Evaluation Context (flagzen-eval-context)

## US-EC-01: EvaluationContext Model

### Problem

Kenji Tanaka is a senior Java developer at a SaaS company who uses FlagZen for polymorphic dispatch. He needs to resolve flags differently per user (A/B testing, tenant-scoped features), but FlagZen has no model to carry targeting information. He currently has no way to tell his FlagProvider "this request is for user X on plan Y."

### Who

- Java developer | SaaS application | Needs per-user/tenant flag targeting

### Solution

An immutable `EvaluationContext` model carrying a targeting key and a flexible attributes map, constructed via builder pattern.

### Domain Examples

#### 1: A/B Test Targeting -- Kenji creates context for user in enterprise plan

Kenji builds `EvaluationContext.builder().targetingKey("user-7291").attribute("plan", "enterprise").attribute("region", "eu-west").build()`. The context is immutable with targeting key "user-7291" and two attributes.

#### 2: Anonymous Context -- Kenji creates context without targeting key

Kenji builds `EvaluationContext.builder().attribute("locale", "de-DE").build()`. The context has null targeting key but carries a locale attribute for feature targeting.

#### 3: Empty Context -- Kenji creates a minimal context

Kenji builds `EvaluationContext.builder().targetingKey("session-abc").build()`. The context has a targeting key but no attributes. The attributes map is empty, not null.

### UAT Scenarios (BDD)

#### Scenario: Build context with targeting key and attributes

Given Kenji Tanaka needs to target flags for user "user-7291" on plan "enterprise"
When he builds an EvaluationContext with targeting key "user-7291" and attributes "plan"="enterprise", "region"="eu-west"
Then the context targeting key is "user-7291"
And the attribute "plan" is "enterprise"
And the attribute "region" is "eu-west"

#### Scenario: Context without targeting key is valid

Given Kenji Tanaka builds an EvaluationContext with only attribute "locale" = "de-DE"
When he inspects the targeting key
Then the targeting key is null
And the context is otherwise functional

#### Scenario: Context has meaningful toString

Given an EvaluationContext with targeting key "user-7291" and attribute "plan" = "enterprise"
When toString() is called
Then the result includes the targeting key and attributes in a human-readable format

#### Scenario: Context equality is based on content

Given two EvaluationContext instances built with identical targeting key "user-7291" and attribute "plan" = "enterprise"
When equals() is called between them
Then they are equal
And their hashCode values match

### Acceptance Criteria

- [ ] EvaluationContext has no setters and returns unmodifiable collections (architectural constraint, verified by unit test)
- [ ] Builder pattern with targetingKey(String) and attribute(String, Object) methods
- [ ] Targeting key is nullable
- [ ] Attributes map is never null (empty map when no attributes set)
- [ ] Meaningful toString(), equals(), hashCode()
- [ ] Zero runtime reflection

### Outcome KPIs

- **Who**: Java developers creating evaluation contexts
- **Does what**: Build an EvaluationContext with correct data in a single fluent chain
- **By how much**: 100% of contexts built in one statement (no multi-step construction)
- **Measured by**: API usage patterns in documentation examples
- **Baseline**: No evaluation context model exists (M0)

### Technical Notes

- Consider implementing as a Java record (Java 17+ supports records) with a static builder
- Attribute values are `Object` to support String, Boolean, Integer, Double, List without type explosion
- Thread-safe by design (immutable)
- No validation on attribute values -- FlagZen is not a rules engine

---

## US-EC-02: Explicit Context on FeatureDispatcher.resolve()

### Problem

Kenji Tanaka has an EvaluationContext but no way to pass it to `FeatureDispatcher.resolve()`. The current API only accepts `Class<T>`, forcing all users to get the same flag value regardless of who they are.

### Who

- Java developer | Has EvaluationContext instance | Wants per-request flag resolution

### Solution

Add `<T> T resolve(Class<T> featureType, EvaluationContext context)` overload to `FeatureDispatcher`.

### Domain Examples

#### 1: Enterprise user gets premium checkout -- Kenji passes context for VIP user

Kenji calls `dispatcher.resolve(CheckoutFlow.class, vipContext)` where vipContext has targeting key "user-vip-42" and attribute "plan"="enterprise". The FlagProvider can use this context to return "PREMIUM".

#### 2: Free-tier user gets classic checkout -- Kenji passes context for standard user

Kenji calls `dispatcher.resolve(CheckoutFlow.class, freeContext)` where freeContext has attribute "plan"="free". The FlagProvider returns "CLASSIC".

#### 3: Existing code without context -- Kenji upgrades without breaking anything

Kenji's existing `dispatcher.resolve(CheckoutFlow.class)` call compiles and works identically to M0 after upgrading to M1.

### UAT Scenarios (BDD)

#### Scenario: Explicit context is forwarded to FlagProvider

Given a FlagProvider that returns "PREMIUM" when context attribute "plan" is "enterprise"
And an EvaluationContext with targeting key "user-vip-42" and attribute "plan" = "enterprise"
When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, context)
Then the FlagProvider receives the context alongside the flag key
And the resolved proxy dispatches to the PREMIUM variant

#### Scenario: Resolve without context compiles after M1 upgrade

Given Kenji Tanaka has existing code: dispatcher.resolve(CheckoutFlow.class)
When he upgrades flagzen-core from M0 to M1
Then the existing call compiles without changes
And behavior is identical to M0

#### Scenario: Null context is treated as no context

Given Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class, null)
When the dispatcher resolves the flag
Then behavior is identical to dispatcher.resolve(CheckoutFlow.class)

### Acceptance Criteria

- [ ] `resolve(Class<T>, EvaluationContext)` overload added to FeatureDispatcher interface
- [ ] Existing `resolve(Class<T>)` unchanged and backward compatible
- [ ] Null context treated as absent (falls through to other resolution paths)
- [ ] Context forwarded to FlagProvider via getString(key, context)

### Outcome KPIs

- **Who**: Java developers passing context to flag resolution
- **Does what**: Resolve flags with per-request context in a single method call
- **By how much**: Zero additional lines of code vs contextless resolve (same call pattern, one extra param)
- **Measured by**: API symmetry -- resolve(Class) and resolve(Class, Context) are the only two overloads
- **Baseline**: Only resolve(Class) exists (M0)

### Technical Notes

- FeatureDispatcher is an interface -- overload is an additional method with default implementation delegating to resolve(Class) for backward compat of custom implementations
- DefaultFeatureDispatcher must implement the new overload
- Depends on: US-EC-01 (EvaluationContext), US-EC-03 (FlagProvider overload)

---

## US-EC-03: FlagProvider Context-Aware Overload

### Problem

Kenji Tanaka's FlagProvider currently only receives `getString(String key)`. When context is passed to `resolve()`, the provider has no way to receive it. Existing providers (InMemoryFlagProvider, env var provider) must not break when this overload is added.

### Who

- FlagProvider implementor | Has existing provider code | Must not break on upgrade

### Solution

Add `Optional<String> getString(String key, EvaluationContext context)` default method to FlagProvider, delegating to `getString(key)` by default.

### Domain Examples

#### 1: LaunchDarkly adapter uses context -- Provider maps EvaluationContext to LD's LDContext

A LaunchDarkly FlagProvider adapter receives the EvaluationContext, maps targeting key to LDContext.key, maps attributes to LDContext.custom, and calls the LD SDK with context-aware evaluation.

#### 2: InMemoryFlagProvider ignores context -- Existing provider works unchanged

InMemoryFlagProvider does not override getString(key, context). The default method delegates to getString(key), ignoring the context. All existing tests pass.

#### 3: Custom provider partially uses context -- Provider checks targeting key only

A custom provider overrides getString(key, context) and checks only the targeting key for simple targeting, ignoring attributes.

### UAT Scenarios (BDD)

#### Scenario: Default method delegates to getString(key)

Given InMemoryFlagProvider only implements getString(String key)
And InMemoryFlagProvider has flag "checkout-flow" = "CLASSIC"
When getString("checkout-flow", someContext) is called
Then the default method delegates to getString("checkout-flow")
And "CLASSIC" is returned

#### Scenario: Provider override uses context

Given a TestContextProvider that overrides getString(key, context)
And it returns "PREMIUM" when context attribute "plan" is "enterprise"
When getString("checkout-flow", enterpriseContext) is called
Then "PREMIUM" is returned based on the context

#### Scenario: Existing providers compile without changes

Given InMemoryFlagProvider source code from M0
When compiled against the M1 FlagProvider interface
Then compilation succeeds without any code changes

### Acceptance Criteria

- [ ] `getString(String key, EvaluationContext context)` added as default method on FlagProvider
- [ ] Default implementation delegates to `getString(key)`
- [ ] Existing FlagProvider implementations compile and work without changes
- [ ] Context-aware providers can override the method to use context

### Outcome KPIs

- **Who**: FlagProvider implementors (library authors, users writing custom providers)
- **Does what**: Upgrade to M1 without modifying existing provider code
- **By how much**: Zero code changes required for existing providers
- **Measured by**: InMemoryFlagProvider compiles and passes all M0 tests unchanged
- **Baseline**: FlagProvider has only getString(String key)

### Technical Notes

- Default method on interface -- Java 8+ feature, well understood
- The default method MUST delegate to getString(key) for backward compat
- This is a SPI change -- must be documented as non-breaking in release notes
- Depends on: US-EC-01 (EvaluationContext type)

---

## US-EC-04: Generated Proxy Passes Context to FlagProvider

### Problem

Kenji Tanaka passes context to `dispatcher.resolve()`, but the generated proxy class still calls `flagProvider.getString(key)` without context. The context is lost between the dispatcher and the flag lookup.

### Who

- Java developer | Uses polymorphic dispatch with context | Expects flag provider to receive context

### Solution

Update the annotation processor's code generator to produce proxies that call `flagProvider.getString(key, context)` when context is available.

### Domain Examples

#### 1: Proxy dispatch with context -- CheckoutFlow proxy uses enterprise context

The generated `CheckoutFlow_FlagZenProxy` receives context via the dispatcher. On each method call, it calls `flagProvider.getString("checkout-flow", context)` instead of `flagProvider.getString("checkout-flow")`.

#### 2: Proxy dispatch without context -- M0 behavior preserved

When no context is available, the proxy calls `flagProvider.getString("checkout-flow")` (the contextless overload), preserving M0 behavior.

#### 3: Proxy with context-unaware provider -- Default method handles gracefully

The proxy calls `flagProvider.getString("checkout-flow", context)`. The provider's default method delegates to `flagProvider.getString("checkout-flow")`, effectively ignoring context. The proxy still dispatches correctly.

### UAT Scenarios (BDD)

#### Scenario: Generated proxy forwards context to FlagProvider

Given a @Feature interface CheckoutFlow with flag key "checkout-flow"
And the generated CheckoutFlow_FlagZenProxy has been created with a context-aware FlagProvider
When a method is called on the proxy with an active EvaluationContext
Then the proxy calls flagProvider.getString("checkout-flow", context)

#### Scenario: Generated proxy works without context

Given a @Feature interface CheckoutFlow
And the generated proxy has no active EvaluationContext
When a method is called on the proxy
Then the proxy calls flagProvider.getString("checkout-flow") without context
And behavior is identical to M0-generated proxies

#### Scenario: Proxy regeneration required after M1 upgrade

Given a project using FlagZen M0 with existing generated proxies
When the project upgrades to M1 and recompiles
Then new proxies are generated with context-passing logic
And all existing tests pass

### Acceptance Criteria

- [ ] Annotation processor generates proxies that accept and forward EvaluationContext
- [ ] Proxy calls getString(key, context) when context is available
- [ ] Proxy calls getString(key) when no context is available
- [ ] Generated code contains zero java.lang.reflect imports
- [ ] Existing @Feature/@Variant annotations require no changes

### Outcome KPIs

- **Who**: Java developers using polymorphic dispatch with context
- **Does what**: Flag provider receives evaluation context transparently through generated proxy
- **By how much**: Zero manual proxy code needed -- annotation processor handles everything
- **Measured by**: Generated proxy source inspection shows getString(key, context) call
- **Baseline**: Generated proxies call getString(key) only (M0)

### Technical Notes

- This modifies ProxyGenerator.java in the annotation processor
- The proxy must receive context somehow -- either stored per-call on the proxy or passed through a resolution context object
- Zero reflection constraint must be maintained
- Depends on: US-EC-02 (resolve overload), US-EC-03 (FlagProvider overload)

---

## US-EC-05: Block-Scoped Context via FlagContext.run()

### Problem

Kenji Tanaka needs evaluation context for multiple resolve() calls within a request handler, but threading an EvaluationContext parameter through every method in the call stack is impractical. He needs a way to scope context to a block of code.

### Who

- Java developer | Request-handling code with multiple resolve() calls | Wants to avoid parameter drilling

### Solution

`FlagContext.run(EvaluationContext, Runnable)` and `FlagContext.run(EvaluationContext, Supplier<T>)` that store context in ThreadLocal for the duration of the block.

### Domain Examples

#### 1: Request handler with multiple features -- Kenji scopes context for Maria Santos's request

```java
EvaluationContext ctx = EvaluationContext.builder()
    .targetingKey("maria-santos-1042")
    .attribute("plan", "enterprise")
    .build();

FlagContext.run(ctx, () -> {
    CheckoutFlow checkout = dispatcher.resolve(CheckoutFlow.class);
    PaymentMethod payment = dispatcher.resolve(PaymentMethod.class);
    // Both resolve calls use Maria's context
});
```

#### 2: Nested scopes -- Inner scope overrides outer for Hiroshi Yamamoto

```java
EvaluationContext outer = EvaluationContext.builder().targetingKey("hiroshi-yamamoto-305").build();
EvaluationContext inner = EvaluationContext.builder().targetingKey("admin-override").build();

FlagContext.run(outer, () -> {
    // Hiroshi's context applies here
    FlagContext.run(inner, () -> {
        // admin-override context applies here
    });
    // Hiroshi's context applies here again
});
```

#### 3: Supplier variant returns result -- Kenji gets resolved proxy back

```java
CheckoutFlow checkout = FlagContext.run(ctx, () ->
    dispatcher.resolve(CheckoutFlow.class)
);
```

### UAT Scenarios (BDD)

#### Scenario: Scoped context applies to resolve calls within block

Given an EvaluationContext with targeting key "maria-santos-1042" and attribute "plan" = "enterprise"
When Kenji Tanaka wraps resolve(CheckoutFlow.class) and resolve(PaymentMethod.class) inside FlagContext.run(context, ...)
Then both flag lookups receive the context with targeting key "maria-santos-1042"

#### Scenario: Context is cleared after block exits

Given a FlagContext.run() block has completed
When Kenji Tanaka calls dispatcher.resolve(CheckoutFlow.class) after the block
Then no scoped context is available

#### Scenario: Nested FlagContext.run uses innermost context

Given an outer FlagContext.run with context targeting key "hiroshi-yamamoto-305"
And an inner FlagContext.run with context targeting key "admin-override"
When resolve is called inside the inner block
Then targeting key "admin-override" is used
When resolve is called after inner block but inside outer
Then targeting key "hiroshi-yamamoto-305" is used

#### Scenario: Supplier variant returns the block's result

Given an EvaluationContext and a Supplier that returns the resolved proxy
When Kenji Tanaka calls FlagContext.run(context, () -> dispatcher.resolve(CheckoutFlow.class))
Then the returned value is the resolved CheckoutFlow proxy

#### Scenario: Exception in block still cleans up context

Given a FlagContext.run() block that throws a RuntimeException
When the exception propagates
Then the scoped context is cleaned up
And subsequent resolve calls do not see the scoped context

### Acceptance Criteria

- [ ] FlagContext.run(EvaluationContext, Runnable) scopes context to block
- [ ] FlagContext.run(EvaluationContext, Supplier) scopes context and returns result
- [ ] Context cleaned up on normal exit and on exception
- [ ] Nested FlagContext.run uses innermost context
- [ ] Thread-safe -- each thread/virtual-thread has its own scope

### Outcome KPIs

- **Who**: Java developers with request-handling code
- **Does what**: Scope evaluation context to a code block without parameter drilling
- **By how much**: Eliminates N parameter-passing changes (where N = depth of call stack)
- **Measured by**: Code review -- no EvaluationContext parameters in internal methods
- **Baseline**: No block-scoping mechanism exists (M0)

### Technical Notes

- Initial implementation uses ThreadLocal (Java 17+)
- ScopedValue optimization is US-EC-08 (Release 2, Java 21+)
- FlagContext should be a final class with static methods -- no instantiation
- Must handle virtual threads correctly (ThreadLocal works but ScopedValue is preferred)
- Depends on: US-EC-01 (EvaluationContext type)

---

## US-EC-06: ContextAccessor SPI

### Problem

Kenji Tanaka wants framework-specific context sources (Reactor Context, Servlet request attributes) to automatically provide evaluation context without explicit passing. He needs an extension point that framework adapter modules can implement.

### Who

- Framework adapter author | Needs to plug in framework-specific context sources | Prepares for M6 reactive modules

### Solution

`ContextAccessor` SPI interface with `getContext()` and `priority()`, discovered via ServiceLoader.

### Domain Examples

#### 1: Reactor ContextAccessor (M6 preview) -- Reads from Reactor subscriber context

A future `ReactorContextAccessor` reads EvaluationContext from `reactor.util.context.Context`. It returns the context stored by a WebFilter that builds it from the HTTP request's user principal.

#### 2: Servlet ContextAccessor -- Reads from HttpServletRequest

A custom `ServletContextAccessor` reads EvaluationContext from a request attribute set by a servlet filter. Priority 200 (lower than reactive, which is at 100).

#### 3: No accessor registered -- Falls through to scoped or default

No ContextAccessor is on the classpath. The resolution chain skips the accessor step and checks for block-scoped or default context.

### UAT Scenarios (BDD)

#### Scenario: ContextAccessor provides context when no explicit context given

Given a TestContextAccessor registered via ServiceLoader with priority 100
And it returns EvaluationContext with targeting key "accessor-user-99"
When dispatcher.resolve(CheckoutFlow.class) is called without explicit context
Then the accessor's context with targeting key "accessor-user-99" is used

#### Scenario: Lower priority accessor is consulted first

Given AccessorA with priority 50 returning targeting key "reactor-user"
And AccessorB with priority 100 returning targeting key "servlet-user"
When dispatcher.resolve(CheckoutFlow.class) is called without explicit context
Then AccessorA (priority 50) is consulted first
And targeting key "reactor-user" is used

#### Scenario: Accessor returning empty is skipped

Given AccessorA with priority 50 returning Optional.empty()
And AccessorB with priority 100 returning targeting key "servlet-user"
When dispatcher.resolve(CheckoutFlow.class) is called
Then AccessorA's empty is skipped
And AccessorB's context with targeting key "servlet-user" is used

#### Scenario: No accessor registered

Given no ContextAccessor implementations on the classpath
When dispatcher.resolve(CheckoutFlow.class) is called without explicit context
Then the accessor step is skipped
And resolution falls through to scoped or default context

### Acceptance Criteria

- [ ] ContextAccessor interface with getContext() returning Optional and priority() returning int
- [ ] Discovered via ServiceLoader from META-INF/services
- [ ] Multiple accessors sorted by priority (lower number = higher priority)
- [ ] First non-empty result wins
- [ ] Empty accessors skipped gracefully
- [ ] No accessor registered = step skipped (no error)

### Outcome KPIs

- **Who**: Framework adapter authors and FlagZen extension developers
- **Does what**: Plug in custom context sources via standard SPI pattern
- **By how much**: One interface + one ServiceLoader file to implement
- **Measured by**: ContextAccessor interface is stable and used by M6 modules
- **Baseline**: No context extension point exists (M0)

### Technical Notes

- This story defines the SPI only -- implementations are in M6 (flagzen-reactor, flagzen-mutiny)
- ServiceLoader caching: accessors are discovered once at dispatcher construction, not per-resolve
- priority() convention: 0-99 = framework-level, 100-199 = application-level, 200+ = fallback
- Depends on: US-EC-01 (EvaluationContext type)

---

## US-EC-07: Context Resolution Order

### Problem

Kenji Tanaka has multiple context sources available (explicit parameter, ContextAccessor, block scope, default). He needs the resolution order to be deterministic, documented, and intuitive -- otherwise, flag resolution becomes unpredictable.

### Who

- Java developer | Multiple context sources active | Needs predictable resolution behavior

### Solution

Deterministic resolution order: explicit parameter > ContextAccessor > block-scoped > default context. Documented in Javadoc and tested exhaustively.

### Domain Examples

#### 1: All sources present -- Explicit wins for Carlos Mendez

Carlos Mendez passes an explicit context with targeting key "carlos-mendez-override". A ContextAccessor would return "accessor-user", FlagContext.run has "scoped-user", and default is "default-user". The FlagProvider receives "carlos-mendez-override".

#### 2: No explicit, accessor active -- Accessor wins for Priya Sharma

Priya Sharma's request is handled inside a reactive pipeline. No explicit context is passed. The ReactorContextAccessor provides her context with targeting key "priya-sharma-2048". The FlagProvider receives "priya-sharma-2048".

#### 3: Nothing configured -- M0 behavior for Ahmed Hassan

Ahmed Hassan has not set up any context. No explicit context, no accessor, no scope, no default. The FlagProvider receives getString(key) without context -- identical to M0.

### UAT Scenarios (BDD)

#### Scenario: Explicit context beats all other sources

Given explicit context with targeting key "carlos-mendez-override"
And a ContextAccessor returning "accessor-user"
And a scoped context with "scoped-user"
And a default context with "default-user"
When dispatcher.resolve(CheckoutFlow.class, explicitContext) is called
Then the FlagProvider receives context with targeting key "carlos-mendez-override"

#### Scenario: Accessor beats scoped and default

Given no explicit context
And a ContextAccessor returning "priya-sharma-2048"
And a scoped context with "scoped-user"
And a default context with "default-user"
When dispatcher.resolve(CheckoutFlow.class) is called
Then the FlagProvider receives context with targeting key "priya-sharma-2048"

#### Scenario: Scoped beats default

Given no explicit context and no ContextAccessor
And a scoped context with "scoped-user"
And a default context with "default-user"
When dispatcher.resolve(CheckoutFlow.class) is called inside FlagContext.run()
Then the FlagProvider receives context with targeting key "scoped-user"

#### Scenario: Default is last resort

Given no explicit context, no ContextAccessor, no scoped context
And a default context configured with targeting key "default-user"
When dispatcher.resolve(CheckoutFlow.class) is called
Then the FlagProvider receives context with targeting key "default-user"

#### Scenario: No context at all -- backward compatible with M0

Given no explicit context, no ContextAccessor, no scoped context, no default context
When dispatcher.resolve(CheckoutFlow.class) is called
Then the FlagProvider receives getString("checkout-flow") without any context

### Acceptance Criteria

- [ ] Resolution order: explicit > accessor > scoped > default
- [ ] Order is documented in FeatureDispatcher Javadoc
- [ ] Each level independently testable
- [ ] No context at all = M0 behavior (getString(key) without context)
- [ ] Resolution order cannot be reconfigured (by design -- simplicity over flexibility)

### Outcome KPIs

- **Who**: Java developers with multiple active context sources
- **Does what**: Predict which context will be used for any given resolve call
- **By how much**: 100% deterministic -- same inputs always produce same context selection
- **Measured by**: Resolution order documented, tested with all 5 permutations
- **Baseline**: No resolution order exists (M0 has no context)

### Technical Notes

- Resolution logic lives in DefaultFeatureDispatcher
- The order is hardcoded, not configurable -- simplicity is a feature
- Accessor + scoped ordering (accessor before scoped) is deliberate: framework context is more specific than thread-scoped context
- Depends on: US-EC-02, US-EC-05, US-EC-06

---

## US-EC-08: ScopedValue Carrier for FlagContext.run() (Java 21+)

### Problem

Kenji Tanaka uses Java 21 with virtual threads. ThreadLocal works but is not ideal for virtual threads -- ScopedValue (Java 21+ preview, finalized in later JDK) is the recommended mechanism for scoped values in virtual thread environments.

### Who

- Java developer | Java 21+ | Uses virtual threads | Wants optimal thread safety

### Solution

FlagContext.run() detects Java 21+ at initialization and uses ScopedValue instead of ThreadLocal. Java 17-20 falls back to ThreadLocal. The API surface is identical.

### Domain Examples

#### 1: Virtual threads with ScopedValue -- Elena Rossi runs on Java 21

Elena Rossi's application uses Java 21 virtual threads. FlagContext.run(ctx, ...) internally uses ScopedValue.where(CONTEXT, ctx).run(...). Context is automatically inherited by child virtual threads within the scope.

#### 2: Classic threads with ThreadLocal -- Tomas Bergstrom runs on Java 17

Tomas Bergstrom's application uses Java 17 with platform threads. FlagContext.run(ctx, ...) internally uses ThreadLocal. Behavior is identical to the user.

#### 3: Mixed environment -- Library works on both JDK versions

FlagZen is published as a Java 17+ library. It detects at class-loading time whether ScopedValue is available and selects the appropriate carrier. No user configuration needed.

### UAT Scenarios (BDD)

#### Scenario: ScopedValue used on Java 21+

Given the application runs on Java 21+
When FlagContext.run(context, ...) is called
Then ScopedValue is used for context storage
And context is properly scoped to the block

#### Scenario: ThreadLocal used on Java 17-20

Given the application runs on Java 17
When FlagContext.run(context, ...) is called
Then ThreadLocal is used for context storage
And context is properly scoped to the block

#### Scenario: API is identical regardless of Java version

Given FlagContext.run(context, runnable) is called
When the Java version changes between 17 and 21
Then the API surface is unchanged
And behavior is identical from the caller's perspective

### Acceptance Criteria

- [ ] ScopedValue used on Java 21+ when available
- [ ] ThreadLocal used as fallback on Java 17-20
- [ ] Detection happens at class-loading time (not per-call)
- [ ] No API surface difference between the two carriers
- [ ] Works correctly with both platform threads and virtual threads

### Outcome KPIs

- **Who**: Java developers on Java 21+ using virtual threads
- **Does what**: Use FlagContext.run() without worrying about ThreadLocal pinning
- **By how much**: Zero virtual thread pinning from FlagContext operations
- **Measured by**: JFR shows no ThreadLocal pinning events in FlagContext code path
- **Baseline**: ThreadLocal-only implementation (US-EC-05)

### Technical Notes

- ScopedValue API may still be preview in some Java 21 releases -- use multi-release JAR or runtime detection
- Consider: ScopedValue.where(key, value).run(Runnable) vs ScopedValue.where(key, value).call(Callable)
- This is a Release 2 story -- correctness is achieved with ThreadLocal in R1
- Depends on: US-EC-05 (FlagContext.run with ThreadLocal)
