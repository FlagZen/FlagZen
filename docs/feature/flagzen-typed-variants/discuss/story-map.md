# Story Map: flagzen-typed-variants

**User: Kenji Tanaka -- Java backend developer using FlagZen for polymorphic dispatch**

## Goal: Dispatch on integer and boolean flag values with compile-time safety, and access typed flag values directly

## Backbone

|      Extend Annotations      |    Validate at Compile Time     |    Generate Typed Proxies    |    Provide Typed Accessors    |
| ---------------------------- | ------------------------------- | ---------------------------- | ----------------------------- |
| Add FeatureType enum         | Validate type consistency       | INT proxy dispatch           | getBoolean() default method   |
| Add type attr to @Feature    | Validate attribute match        | BOOLEAN proxy dispatch       | getInt() default method       |
| Add intValue to @Variant     | Validate boolean REQUIRED       | Typed variant map gen        | getLong() default method      |
| Add booleanValue to @Variant | Validate duplicate typed values | Context-aware typed dispatch | getDouble() default method    |
|                              |                                 |                              | Context-aware typed accessors |

---

### Walking Skeleton

Thinnest end-to-end slice that proves typed dispatch works:

1. **Extend Annotations**: FeatureType enum + type attribute on @Feature + intValue on @Variant
2. **Validate at Compile Time**: Validate @Variant attribute matches @Feature type (INT case only)
3. **Generate Typed Proxies**: INT proxy dispatch using getInt()
4. **Provide Typed Accessors**: getInt() default method on FlagProvider (parses from getString)

This delivers: a developer can annotate a feature with `FeatureType.INT`, annotate variants with `intValue`, get compile-time validation, and have the proxy dispatch on integer values. One type, end-to-end.

### Release 1: Boolean dispatch and REQUIRED completeness

**Outcome**: Developer can also dispatch on boolean flags with compile-time completeness checking.

- **Extend Annotations**: booleanValue on @Variant
- **Validate at Compile Time**: Boolean REQUIRED completeness (exactly true + false)
- **Generate Typed Proxies**: BOOLEAN proxy dispatch using getBoolean()
- **Provide Typed Accessors**: getBoolean() default method on FlagProvider

### Release 2: Full conditional API

**Outcome**: Developer can use typed accessors for non-polymorphic flag access across all numeric types.

- **Provide Typed Accessors**: getLong() default method
- **Provide Typed Accessors**: getDouble() default method
- **Provide Typed Accessors**: Context-aware typed accessor overloads

## Scope Assessment: PASS -- 7 stories, 2 contexts (annotation model + FlagProvider SPI), estimated 5-7 days
