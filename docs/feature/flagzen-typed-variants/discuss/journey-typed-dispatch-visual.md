# Journey: Typed Polymorphic Dispatch and Conditional API

## Persona

**Kenji Tanaka** -- Java backend developer, 4 years experience. Uses FlagZen for string-based polymorphic dispatch in a payments microservice. Comfortable with annotation processing. Wants to dispatch on integer and boolean flag values instead of encoding everything as strings.

## Emotional Arc

```
Curious         Focused          Confident        Satisfied
"Can I dispatch  "Let me set     "Processor       "Cleaner than
on int/bool?"    up the types"   caught my typo"  string hacks"
    |               |                |                |
    v               v                v                v
[DISCOVER]  --> [ANNOTATE]  --> [VALIDATE]  --> [RESOLVE]
```

## Journey Flow

```
+-----------------------------------------------------------------+
| Step 1: DISCOVER typed dispatch                                 |
|                                                                 |
| Kenji reads docs, sees @Feature supports type = FeatureType.INT |
| Sees @Variant(intValue = ...) and @Variant(booleanValue = ...)  |
|                                                                 |
| Feels: Curious --> "This solves my retry count dispatch hack"   |
+-----------------------------------------------------------------+
|                                                                 |
        v
+-------------------------------------------------------------+
| Step 2: ANNOTATE feature with typed variants                |
|                                                             |
| @Feature(value = "max-retries", type = FeatureType.INT)     |
| interface RetryStrategy { void execute(Request req); }      |
|                                                             |
| @Variant(intValue = 3)                                      |
| class ConservativeRetry implements RetryStrategy { ... }    |
|                                                             |
| @Variant(intValue = 10)                                     |
| class AggressiveRetry implements RetryStrategy { ... }      |
|                                                             |
| Feels: Focused --> "Same pattern I know, just typed values" |
+-------------------------------------------------------------+
|                                                             |
        v
+----------------------------------------------------------------+
| Step 3: VALIDATE at compile time                               |
|                                                                |
| Processor checks:                                              |
| - All @Variant annotations use intValue (not value/boolValue)  |
| - @Feature(type = INT) matches @Variant(intValue = ...)        |
| - No duplicate intValue across variants                        |
| - Boolean features have exactly 2 variants (true + false)      |
| when REQUIRED strategy is used                                 |
|                                                                |
| ERROR PATH: @Variant(value = "3") on INT feature               |
| --> "error: Feature 'max-retries' declares type INT but        |
| variant ConservativeRetry uses string value. Use               |
| @Variant(intValue = 3) instead."                               |
|                                                                |
| Feels: Confident --> "Processor catches type mismatches early" |
+----------------------------------------------------------------+
|                                                                |
        v
+--------------------------------------------------------------+
| Step 4: RESOLVE via typed proxy dispatch                     |
|                                                              |
| Generated proxy calls flagProvider.getInt("max-retries")     |
| instead of getString(). Maps int value to variant instance.  |
|                                                              |
| For boolean features:                                        |
| flagProvider.getBoolean("dark-mode") --> true/false dispatch |
|                                                              |
| Feels: Satisfied --> "No more parseInt hacks in my provider" |
+--------------------------------------------------------------+

## Conditional API (Non-Polymorphic) -- Parallel Journey

+------------------------------------------------------------+
| Step A: USE conditional API directly                       |
|                                                            |
| Kenji uses FlagProvider for simple boolean/int checks:     |
|                                                            |
| FlagProvider flags = ...;                                  |
| if (flags.getBoolean("feature-x-enabled").orElse(false)) { |
| // do feature X                                            |
| }                                                          |
|                                                            |
| int maxItems = flags.getInt("max-items").orElse(100);      |
|                                                            |
| Default methods on FlagProvider parse from getString():    |
| getBoolean --> getString --> Boolean.parseBoolean          |
| getInt     --> getString --> Integer.parseInt              |
| getLong    --> getString --> Long.parseLong                |
| getDouble  --> getString --> Double.parseDouble            |
|                                                            |
| ERROR PATH: Non-parseable value                            |
| getString("max-items") returns "abc"                       |
| getInt("max-items") returns Optional.empty()               |
| (parse failure = absent, not exception)                    |
|                                                            |
| Feels: Satisfied --> "Clean API, no manual parsing"        |
+------------------------------------------------------------+
```

## Error Paths Summary

|                         Error                         |     When     |                                User Sees                                 |                   Recovery                    |
| ----------------------------------------------------- | ------------ | ------------------------------------------------------------------------ | --------------------------------------------- |
| Type mismatch: string @Variant on INT feature         | Compile time | Processor error with fix suggestion                                      | Change to intValue/booleanValue               |
| Attribute mismatch: intValue on BOOLEAN feature       | Compile time | Processor error with fix suggestion                                      | Change to booleanValue                        |
| Mixed types: some variants use intValue, others value | Compile time | Processor error listing inconsistent variants                            | Unify all to same type                        |
| Boolean REQUIRED incomplete: only true variant        | Compile time | Processor error: "boolean feature requires both true and false variants" | Add missing variant                           |
| Duplicate typed values: two variants with intValue=3  | Compile time | Same as existing duplicate variant error                                 | Remove duplicate                              |
| Non-parseable flag value at runtime                   | Runtime      | getInt/getBoolean returns Optional.empty()                               | Provider returns correct type, or use default |
| FlagProvider.getInt returns empty for INT feature     | Runtime      | Falls through to fallback strategy (same as string)                      | Configure default variant or check provider   |
