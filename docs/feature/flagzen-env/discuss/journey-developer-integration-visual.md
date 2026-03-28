# Journey Visual: Environment Variable Provider Integration

## Personas

**Kenji Tanaka** -- backend developer at a SaaS company building 12-factor apps. Deploys to Kubernetes.
Wants flag values driven by environment variables so ops can toggle features per environment without code changes or external flag services.

**Mei-Lin Chen** -- platform engineer managing a shared Kubernetes cluster for multiple teams.
Wants custom parsers/formatters and conflict control to fit existing conventions and avoid collisions.

## Emotional Arc

```
Curious ──> Clear ──> Confident ──> Satisfied
  |           |           |              |
"Can I use  "Defaults   "I can         "It just works,
 env vars    are         customize      fast lookups,
 with        obvious"    parsers,       conflict-safe"
 FlagZen?"               formatters,
                         and conflict
                         strategy"
```

## Journey Flow

```
[1. Add Dependency]──>[2. Set Env Vars]──>[3. Configure Provider]──>[4. Verify]
   Feels: Curious      Feels: Clear        Feels: Confident          Feels: Satisfied
   Sees: build.gradle  Sees: shell/K8s     Sees: create()/builder    Sees: flag resolves
                       Knows: parse-format  Knows: parser + formatter from immutable map
                       pipeline             + conflict strategy
```

## Module Structure

```
flagzen-key-mapping (reusable across providers)
├── FlagKeyParser       (SAM: env var name -> Optional<List<String>> segments)
├── FlagKeyParsers      (built-in: screamingSnakeCase, camelCase)
├── FlagKeyFormat       (SAM: segments -> flag key string)
├── FlagKeyFormats      (built-in: kebab, snake, camel, pascal, dot, colon)
└── ConflictStrategy    (WARN / ERROR)

flagzen-env (env var provider)
├── EnvironmentVariableFlagProvider  (FlagProvider impl + builder)
├── ServiceLoader registration       (META-INF/services)
└── depends on: flagzen-key-mapping, flagzen-core
```

## Architecture: Parse-Format Pipeline

```
┌──────────────────────────────────────────────────────────────────────┐
│                    CONSTRUCTION TIME (eager)                         │
│                                                                      │
│  System.getenv()                                                     │
│       │                                                              │
│       ▼                                                              │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐            │
│  │ FlagKeyParser │───>│  Segments    │───>│ FlagKeyFormat│            │
│  │ (input)      │    │ List<String> │    │ (output)     │            │
│  └─────────────┘    └──────────────┘    └──────────────┘            │
│       │                                        │                     │
│       │  "FLAGZEN_CHECKOUT_FLOW"               │  "checkout-flow"   │
│       │  ──> ["checkout", "flow"]              │  = "PREMIUM"       │
│       │                                        │                     │
│       ▼                                        ▼                     │
│  ┌──────────────────────────────────────────────────┐               │
│  │        Immutable Map: flagKey -> envVarValue      │               │
│  │   "checkout-flow" -> "PREMIUM"                    │               │
│  │   "dark-mode"     -> "true"                       │               │
│  │   "max-retries"   -> "5"                          │               │
│  └──────────────────────────────────────────────────┘               │
│                           │                                          │
│  ConflictStrategy ────────┤  (WARN: log + keep last)                │
│                           │  (ERROR: throw IllegalStateException)   │
└───────────────────────────┼──────────────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────────────┐
│                    RUNTIME (pure map lookup)                         │
│                            │                                         │
│  getString("checkout-flow") ──> Map.get("checkout-flow")            │
│                                  ──> Optional.of("PREMIUM")         │
│                                                                      │
│  (no System.getenv() at runtime)                                    │
└──────────────────────────────────────────────────────────────────────┘
```

## Step Details

### Step 1: Add Dependency

```
build.gradle:
┌──────────────────────────────────────────────────────────┐
│ dependencies {                                           │
│     implementation 'com.flagzen:flagzen-env:1.1.0'       │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
```

Emotional state: Curious -> Clear ("one line, standard Gradle -- flagzen-key-mapping comes transitively")

### Step 2: Set Environment Variables

```
Terminal (default convention -- FLAGZEN_ prefix + SCREAMING_SNAKE_CASE):
┌──────────────────────────────────────────────────────────┐
│ $ export FLAGZEN_CHECKOUT_FLOW=PREMIUM                   │
│ $ export FLAGZEN_DARK_MODE=true                          │
│ $ export FLAGZEN_MAX_RETRIES=5                           │
└──────────────────────────────────────────────────────────┘

Kubernetes ConfigMap (custom prefix -- FF_):
┌──────────────────────────────────────────────────────────┐
│ apiVersion: v1                                           │
│ kind: ConfigMap                                          │
│ data:                                                    │
│   FF_CHECKOUT_FLOW: "PREMIUM"                            │
│   FF_MAX_RETRIES: "5"                                    │
└──────────────────────────────────────────────────────────┘

Parse-Format Pipeline Examples:
┌──────────────────────────────────────────────────────────┐
│  Env Var Name          Parser              Segments      │
│  ──────────────────    ─────────────────   ──────────── │
│  FLAGZEN_CHECKOUT_FLOW screamingSnake      ["checkout",  │
│                        Case("FLAGZEN_")     "flow"]      │
│                                                          │
│  myAppCheckoutFlow     camelCase("myApp")  ["checkout",  │
│                                             "flow"]      │
│                                                          │
│  Segments              Formatter            Flag Key     │
│  ──────────────────    ─────────────────   ──────────── │
│  ["checkout", "flow"]  kebabCase()         checkout-flow │
│  ["checkout", "flow"]  snakeCase()         checkout_flow │
│  ["checkout", "flow"]  camelCase()         checkoutFlow  │
│  ["checkout", "flow"]  dotCase()           checkout.flow │
└──────────────────────────────────────────────────────────┘
```

Emotional state: Clear -> Confident ("parse-format pipeline is predictable, customizable")

### Step 3: Configure Provider

```
Java code:
┌──────────────────────────────────────────────────────────┐
│ // Option A: Zero config -- sensible defaults            │
│ var provider = EnvironmentVariableFlagProvider.create();  │
│                                                          │
│ // Option B: Custom prefix                               │
│ var provider = EnvironmentVariableFlagProvider.builder()  │
│     .parser(FlagKeyParsers.screamingSnakeCase("FF_"))     │
│     .formatter(FlagKeyFormats.kebabCase())                │
│     .build();                                            │
│                                                          │
│ // Option C: Multiple parsers (legacy migration)         │
│ var provider = EnvironmentVariableFlagProvider.builder()  │
│     .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))│
│     .parser(FlagKeyParsers.camelCase("myApp"))            │
│     .formatter(FlagKeyFormats.kebabCase())                │
│     .onConflict(ConflictStrategy.WARN)                   │
│     .build();                                            │
│                                                          │
│ // Option D: Multiple formatters (multi-convention)      │
│ var provider = EnvironmentVariableFlagProvider.builder()  │
│     .parser(FlagKeyParsers.screamingSnakeCase("FLAGZEN_"))│
│     .formatter(FlagKeyFormats.kebabCase())                │
│     .formatter(FlagKeyFormats.snakeCase())                │
│     .build();                                            │
│                                                          │
│ // Option E: Custom lambda parser                        │
│ var provider = EnvironmentVariableFlagProvider.builder()  │
│     .parser(name -> name.startsWith("FEAT_")             │
│         ? Optional.of(Arrays.asList(                     │
│             name.substring(5).toLowerCase().split("_"))) │
│         : Optional.empty())                              │
│     .formatter(FlagKeyFormats.kebabCase())                │
│     .build();                                            │
└──────────────────────────────────────────────────────────┘
```

Emotional state: Confident ("sensible defaults, full control when needed")

### Step 4: Verify Resolution

```
Java code + output:
┌──────────────────────────────────────────────────────────┐
│ // Default config (zero-config)                          │
│ var provider = EnvironmentVariableFlagProvider.create();  │
│ provider.getString("checkout-flow");                     │
│ // -> Optional.of("PREMIUM")                             │
│ // (from FLAGZEN_CHECKOUT_FLOW=PREMIUM)                  │
│                                                          │
│ // Custom prefix                                         │
│ provider.getString("checkout-flow");                     │
│ // -> Optional.of("PREMIUM")                             │
│ // (from FF_CHECKOUT_FLOW=PREMIUM)                       │
│                                                          │
│ // Multiple formatters                                   │
│ provider.getString("checkout-flow");  // kebab           │
│ provider.getString("checkout_flow");  // snake           │
│ // Both -> Optional.of("PREMIUM")                        │
│ // (from single env var FLAGZEN_CHECKOUT_FLOW)           │
│                                                          │
│ // Missing flag                                          │
│ provider.getString("nonexistent");                       │
│ // -> Optional.empty()                                   │
└──────────────────────────────────────────────────────────┘
```

Emotional state: Satisfied ("env vars resolve correctly, fast O(1) lookups")

## Error Paths

### Missing flag key

```
┌──────────────────────────────────────────────────────────┐
│ provider.getString("nonexistent");                       │
│ // -> Optional.empty()                                   │
│ // No exception, no noise -- consistent with FlagProvider│
└──────────────────────────────────────────────────────────┘
```

### Unparseable typed value

```
┌──────────────────────────────────────────────────────────┐
│ $ export FLAGZEN_MAX_RETRIES=not-a-number                │
│                                                          │
│ provider.getInt("max-retries");                          │
│ // -> OptionalInt.empty()                                │
│ provider.getString("max-retries");                       │
│ // -> Optional.of("not-a-number")                        │
└──────────────────────────────────────────────────────────┘
```

### Conflict: WARN strategy

```
┌──────────────────────────────────────────────────────────┐
│ // Two parsers, both map to "checkout-flow"              │
│ // FLAGZEN_CHECKOUT_FLOW=PREMIUM                         │
│ // myAppCheckoutFlow=BASIC                               │
│                                                          │
│ // At construction:                                      │
│ // WARN: Flag key 'checkout-flow' mapped from multiple   │
│ //   env vars: FLAGZEN_CHECKOUT_FLOW, myAppCheckoutFlow  │
│                                                          │
│ // On first getString("checkout-flow"):                  │
│ // WARN: Accessing conflicted flag key 'checkout-flow'   │
│ //   (resolved from: FLAGZEN_CHECKOUT_FLOW,              │
│ //    myAppCheckoutFlow)                                 │
└──────────────────────────────────────────────────────────┘
```

### Conflict: ERROR strategy

```
┌──────────────────────────────────────────────────────────┐
│ // Two parsers + two formatters, conflict detected       │
│ // At construction:                                      │
│ // throws IllegalStateException:                         │
│ //   "Flag key 'checkout-flow' mapped from multiple      │
│ //    env vars: FLAGZEN_CHECKOUT_FLOW, FF_CHECKOUT_FLOW" │
│                                                          │
│ // Service fails to start -- fail-fast                   │
└──────────────────────────────────────────────────────────┘
```

## Built-in Reference

```
┌──────────────────────────────────────────────────────────┐
│ PARSERS (env var name -> segments)  [flagzen-key-mapping] │
│ ────────────────────────────────────────────             │
│ FlagKeyParsers.screamingSnakeCase("FLAGZEN_")             │
│   FLAGZEN_CHECKOUT_FLOW -> ["checkout", "flow"]          │
│                                                          │
│ FlagKeyParsers.screamingSnakeCase()                       │
│   CHECKOUT_FLOW -> ["checkout", "flow"]                  │
│                                                          │
│ FlagKeyParsers.camelCase("myApp")                         │
│   myAppCheckoutFlow -> ["checkout", "flow"]              │
│                                                          │
│ FlagKeyParsers.camelCase()                                │
│   checkoutFlow -> ["checkout", "flow"]                   │
│                                                          │
│ Custom lambda:                                           │
│   name -> Optional.of(List.of("segments"...))            │
├──────────────────────────────────────────────────────────┤
│ FORMATTERS (segments -> flag key)  [flagzen-key-mapping]  │
│ ────────────────────────────────────────────             │
│ FlagKeyFormats.kebabCase()    -> "checkout-flow"         │
│ FlagKeyFormats.snakeCase()    -> "checkout_flow"         │
│ FlagKeyFormats.camelCase()    -> "checkoutFlow"          │
│ FlagKeyFormats.pascalCase()   -> "CheckoutFlow"          │
│ FlagKeyFormats.dotCase()      -> "checkout.flow"         │
│ FlagKeyFormats.colonCase()    -> "checkout:flow"         │
│                                                          │
│ Custom lambda:                                           │
│   segments -> String.join("/", segments)                 │
├──────────────────────────────────────────────────────────┤
│ CONFLICT STRATEGY                  [flagzen-key-mapping]  │
│ ────────────────────────────────────────────             │
│ WARN  -- log warning, last mapping wins (default)        │
│ ERROR -- throw IllegalStateException (default for N×M)  │
│                                                          │
│ Default rules:                                           │
│   1 parser + 1 formatter -> WARN                         │
│   N parsers + 1 formatter -> WARN                        │
│   1 parser + M formatters -> WARN                        │
│   N parsers + M formatters -> ERROR (override with WARN) │
└──────────────────────────────────────────────────────────┘
```
