# Walking Skeleton Rationale: flagzen-multi-value-variant

## Skeletons Selected: 2

### Skeleton 1: Compile-time string array annotation

**User goal**: Developer maps multiple string flag values to one variant implementation using array syntax.

**Why this slice**: Thinnest path through the annotation schema change (`String[] value()`), processor array expansion (`processVariantAnnotation()`), and proxy generation. Proves the end-to-end compilation pipeline works with multi-value arrays.

**Observable outcome**: Compilation succeeds, and the generated proxy maps all array values to the same implementation class. Stakeholder can see: "yes, the developer wrote one annotation with two values and both are registered."

**Driving ports**: Java compiler + FlagZenProcessor (compile-time).

### Skeleton 2: Runtime dispatch for multi-value string

**User goal**: Developer resolves a feature at runtime and gets the correct implementation when the flag value matches any value in the array.

**Why this slice**: Proves the runtime dispatch path works with multi-value mappings. The flag provider returns "LEGACY" (second array element), and the dispatcher returns `ClassicCheckout`.

**Observable outcome**: The correct variant handles the method call. Stakeholder can see: "yes, the second value in the array routes to the right implementation."

**Driving ports**: FeatureDispatcher.resolve() (runtime), FlagProvider.getString() (flag source).

## Why Not More Skeletons

Two skeletons cover both halves of the developer experience: compile-time (annotation works) and runtime (dispatch works). Int/long array expansion is mechanically identical to string and does not warrant a separate skeleton. `@CloseTo` overlap detection is a validation concern, not a user journey.

## Litmus Test

1. Title describes user goal? YES -- "Developer maps multiple string values to one variant implementation"
2. Then steps describe user observations? YES -- "the generated proxy maps X to Y" / "the variant handles the call"
3. Non-technical stakeholder can confirm? YES -- "I wrote one annotation with two values and it works at compile and runtime"
