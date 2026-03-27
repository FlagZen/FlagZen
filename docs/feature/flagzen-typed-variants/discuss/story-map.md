# Story Map: flagzen-typed-variants

## User: Carlos Mendes / Mei Chen -- Java developers using FlagZen for polymorphic dispatch and conditional flag access

## Goal: Dispatch on integer, boolean, long, and double flag values with compile-time safety, and access typed flag values directly via FlagProvider

## Backbone

| Extend Annotations | Validate at Compile Time | Generate Typed Proxies | Provide Typed Accessors |
| ------------------- | ------------------------- | ----------------------- | ------------------------ |
| FeatureType enum (STRING, INT, LONG, BOOLEAN, DOUBLE) | Validate attribute matches @Feature type | INT proxy dispatch (map lookup) | `getBoolean()` default method |
| `type` attribute on @Feature | Validate all variants use same type | LONG proxy dispatch (map lookup) | `getInt()` default method |
| `intValue` on @Variant | Validate BOOLEAN REQUIRED completeness | BOOLEAN proxy dispatch (map lookup) | `getLong()` default method |
| `booleanValue` on @Variant | Validate duplicate typed values | DOUBLE proxy dispatch (iterate + delta) | `getDouble()` default method |
| `longValue` on @Variant | | Context-aware typed dispatch | Context-aware typed accessors |
| `doubleValue` + @CloseTo on @Variant | | | |
| @WhenTrue / @WhenFalse annotations | | | |

---

### Release 1: INT and BOOLEAN typed dispatch (end-to-end)

**Outcome**: Developer can annotate features with `FeatureType.INT` or `FeatureType.BOOLEAN`, annotate variants with `intValue`/`booleanValue`/`@WhenTrue`/`@WhenFalse`, get compile-time validation, and have proxies dispatch on typed values. Conditional API provides `getBoolean()` and `getInt()`.

- **Extend Annotations**: FeatureType enum (STRING, INT, BOOLEAN) + `type` attribute on @Feature + `intValue`/`booleanValue` on @Variant + @WhenTrue/@WhenFalse
- **Validate at Compile Time**: Attribute match, BOOLEAN REQUIRED completeness, duplicate typed values
- **Generate Typed Proxies**: INT proxy dispatch (map lookup) + BOOLEAN proxy dispatch (map lookup)
- **Provide Typed Accessors**: `getBoolean()` + `getInt()` default methods with context-aware overloads

### Release 2: LONG and DOUBLE typed dispatch + full conditional API

**Outcome**: Developer can dispatch on long and double flag values. Double dispatch uses approximate matching via `@CloseTo`. Conditional API is complete with `getLong()` and `getDouble()`.

- **Extend Annotations**: `longValue` on @Variant + `doubleValue` + @CloseTo on @Variant + FeatureType extended with LONG and DOUBLE
- **Validate at Compile Time**: Validation extended for LONG/DOUBLE attribute matching and duplicate detection
- **Generate Typed Proxies**: LONG proxy dispatch (map lookup) + DOUBLE proxy dispatch (iterate + delta) + context-aware typed dispatch
- **Provide Typed Accessors**: `getLong()` + `getDouble()` default methods with context-aware overloads

## Scope Assessment: PASS -- 8 stories, 2 contexts (annotation model + FlagProvider SPI), estimated 7-9 days
