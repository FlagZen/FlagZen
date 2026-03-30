package com.flagzen.processor;

import com.flagzen.FeatureType;
import com.flagzen.Variant;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates variant annotations and models during annotation processing.
 * Package-private — used only by {@link FlagZenProcessor}.
 */
class VariantValidator {

    private static final Map<FeatureType, List<String>> WRONG_ATTR_LABELS = Map.of(
            FeatureType.INT, List.of("string", "long", "double", "boolean"),
            FeatureType.LONG, List.of("string", "integer", "double", "boolean"),
            FeatureType.DOUBLE, List.of("string", "integer", "long", "boolean"),
            FeatureType.BOOLEAN, List.of("string", "integer", "long", "double"),
            FeatureType.STRING, List.of(/* no string slot */"", "integer", "long", "double")
    );

    private final ProcessingEnvironment processingEnv;

    VariantValidator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    boolean hasDuplicateVariantValues(List<VariantModel> variants, String flagKey,
                                      TypeElement featureElement) {
        Map<String, List<String>> valueToClassNames = new HashMap<>();
        for (VariantModel variant : variants) {
            String simpleName = variant.qualifiedClassName()
                    .substring(variant.qualifiedClassName().lastIndexOf('.') + 1);
            String key = variant.variantKeyLiteral();
            valueToClassNames
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(simpleName);
        }
        boolean foundDuplicate = false;
        for (Map.Entry<String, List<String>> entry : valueToClassNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                String classNames = String.join(" and ", entry.getValue());
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Duplicate @Variant(\"" + entry.getKey() + "\") for feature \""
                                + flagKey + "\". Found on: " + classNames,
                        featureElement
                );
                foundDuplicate = true;
            }
        }
        return foundDuplicate;
    }

    boolean hasDuplicateOrderValues(List<VariantModel> variants, String flagKey,
                                     TypeElement featureElement) {
        Map<Integer, List<String>> orderToClassNames = new HashMap<>();
        for (VariantModel variant : variants) {
            if (variant.order() == Integer.MAX_VALUE) {
                continue;
            }
            String simpleName = variant.qualifiedClassName()
                    .substring(variant.qualifiedClassName().lastIndexOf('.') + 1);
            orderToClassNames
                    .computeIfAbsent(variant.order(), k -> new ArrayList<>())
                    .add(simpleName);
        }
        boolean foundDuplicate = false;
        for (Map.Entry<Integer, List<String>> entry : orderToClassNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                String classNames = String.join(" and ", entry.getValue());
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Duplicate order value " + entry.getKey() + " for feature \""
                                + flagKey + "\". Found on: " + classNames,
                        featureElement
                );
                foundDuplicate = true;
            }
        }
        return foundDuplicate;
    }

    boolean hasOverlappingCloseToRanges(List<VariantModel> variants,
                                         TypeElement featureElement) {
        List<VariantModel> doubleVariants = variants.stream()
                .filter(v -> v.featureType() == FeatureType.DOUBLE)
                .toList();

        boolean foundOverlap = false;

        // Check intra-class overlaps first (within same variant class)
        Map<String, List<VariantModel>> byClass = new HashMap<>();
        for (VariantModel v : doubleVariants) {
            byClass.computeIfAbsent(v.qualifiedClassName(), k -> new ArrayList<>()).add(v);
        }
        for (Map.Entry<String, List<VariantModel>> entry : byClass.entrySet()) {
            List<VariantModel> classVariants = entry.getValue();
            if (classVariants.size() < 2) {
                continue;
            }
            String simpleName = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
            for (int i = 0; i < classVariants.size(); i++) {
                for (int j = i + 1; j < classVariants.size(); j++) {
                    if (rangesOverlap(classVariants.get(i), classVariants.get(j))) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "Overlapping @CloseTo ranges within variant " + simpleName
                                        + ": " + formatRange(classVariants.get(i))
                                        + " and " + formatRange(classVariants.get(j))
                                        + ". Consider: reduce delta or remove the redundant entry.",
                                featureElement
                        );
                        foundOverlap = true;
                    }
                }
            }
        }

        // Check inter-class overlaps (across different variant classes)
        for (int i = 0; i < doubleVariants.size(); i++) {
            for (int j = i + 1; j < doubleVariants.size(); j++) {
                VariantModel v1 = doubleVariants.get(i);
                VariantModel v2 = doubleVariants.get(j);
                if (v1.qualifiedClassName().equals(v2.qualifiedClassName())) {
                    continue;
                }
                if (rangesOverlap(v1, v2)) {
                    String name1 = v1.qualifiedClassName()
                            .substring(v1.qualifiedClassName().lastIndexOf('.') + 1);
                    String name2 = v2.qualifiedClassName()
                            .substring(v2.qualifiedClassName().lastIndexOf('.') + 1);
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "Overlapping @CloseTo ranges between " + name1 + " " + formatRange(v1)
                                    + " and " + name2 + " " + formatRange(v2)
                                    + ". Consider: reduce delta or merge variants.",
                            featureElement
                    );
                    foundOverlap = true;
                }
            }
        }

        return foundOverlap;
    }

    boolean hasIncompleteVariantCoverage(List<VariantModel> variants,
                                          List<String> variantEnumValues,
                                          String flagKey,
                                          TypeElement featureElement) {
        Set<String> coveredValues = new HashSet<>();
        for (VariantModel variant : variants) {
            coveredValues.add(variant.variantValue());
        }
        boolean incomplete = false;
        for (String enumValue : variantEnumValues) {
            if (!coveredValues.contains(enumValue)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Feature \"" + flagKey + "\" uses REQUIRED fallback but variant "
                                + enumValue + " has no implementation.",
                        featureElement
                );
                incomplete = true;
            }
        }
        return incomplete;
    }

    boolean hasIncompleteBooleanCoverage(List<VariantModel> variants, String flagKey,
                                          TypeElement featureElement) {
        boolean hasTrue = false;
        boolean hasFalse = false;
        for (VariantModel variant : variants) {
            if (variant.featureType() == FeatureType.BOOLEAN) {
                if (variant.booleanVariantValue()) {
                    hasTrue = true;
                } else {
                    hasFalse = true;
                }
            }
        }
        boolean incomplete = false;
        if (!hasTrue) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Feature \"" + flagKey + "\" uses REQUIRED fallback with BOOLEAN type "
                            + "but is missing a variant for true. "
                            + "Add a @WhenTrue or @Variant(booleanValue=\"true\") variant, "
                            + "or add a @DefaultVariant.",
                    featureElement
            );
            incomplete = true;
        }
        if (!hasFalse) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Feature \"" + flagKey + "\" uses REQUIRED fallback with BOOLEAN type "
                            + "but is missing a variant for false. "
                            + "Add a @WhenFalse or @Variant(booleanValue=\"false\") variant, "
                            + "or add a @DefaultVariant.",
                    featureElement
            );
            incomplete = true;
        }
        return incomplete;
    }

    boolean hasTypeMismatch(Variant annotation, FeatureType featureType,
                             String variantName, String flagKey, Element variantElement) {
        boolean hasString = annotation.value().length > 0;
        boolean hasInt = annotation.intValue().length > 0;
        boolean hasLong = annotation.longValue().length > 0;
        boolean hasDouble = annotation.doubleValue().length > 0;
        boolean hasBoolean = !annotation.booleanValue().isEmpty();

        return switch (featureType) {
            case INT -> checkMismatch(hasInt, "intValue",
                    hasString, hasLong, hasDouble, hasBoolean,
                    variantName, flagKey, featureType, variantElement);
            case LONG -> checkMismatch(hasLong, "longValue",
                    hasString, hasInt, hasDouble, hasBoolean,
                    variantName, flagKey, featureType, variantElement);
            case DOUBLE -> checkMismatch(hasDouble, "doubleValue (with @CloseTo)",
                    hasString, hasInt, hasLong, hasBoolean,
                    variantName, flagKey, featureType, variantElement);
            case BOOLEAN -> checkMismatch(hasBoolean, "booleanValue or @WhenTrue/@WhenFalse",
                    hasString, hasInt, hasLong, hasDouble,
                    variantName, flagKey, featureType, variantElement);
            case STRING -> checkMismatch(hasString, "value",
                    false, hasInt, hasLong, hasDouble,
                    variantName, flagKey, featureType, variantElement);
        };
    }

    void validateVariantValuesAgainstEnum(List<VariantModel> variants,
                                           List<String> validValues,
                                           String interfaceName) {
        for (VariantModel variant : variants) {
            if (!validValues.contains(variant.variantValue())) {
                String validValuesStr = String.join(", ", validValues);
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@Variant(\"" + variant.variantValue() + "\") does not match any value in "
                                + interfaceName + ".Variant. Valid values: " + validValuesStr
                );
            }
        }
    }

    boolean validatePredicateType(TypeMirror predicateMirror, FeatureType featureType,
                                   Element variantElement) {
        String expectedInterface = switch (featureType) {
            case STRING -> "java.util.function.Predicate";
            case INT -> "java.util.function.IntPredicate";
            case LONG -> "java.util.function.LongPredicate";
            case DOUBLE -> "java.util.function.DoublePredicate";
            case BOOLEAN -> null;
        };
        if (expectedInterface == null) {
            return true;
        }

        TypeElement predicateElement = (TypeElement) processingEnv.getTypeUtils()
                .asElement(predicateMirror);
        if (predicateElement == null) {
            return true;
        }

        TypeElement expectedElement = processingEnv.getElementUtils()
                .getTypeElement(expectedInterface);
        if (expectedElement == null) {
            return true;
        }

        TypeMirror expectedRaw = processingEnv.getTypeUtils().erasure(expectedElement.asType());
        boolean assignable = false;
        for (TypeMirror iface : predicateElement.getInterfaces()) {
            TypeMirror erased = processingEnv.getTypeUtils().erasure(iface);
            if (processingEnv.getTypeUtils().isAssignable(erased, expectedRaw)) {
                assignable = true;
                break;
            }
        }

        if (!assignable) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Predicate class " + predicateElement.getSimpleName()
                            + " must implement " + expectedInterface
                            + " for feature type " + featureType + ".",
                    variantElement
            );
            return false;
        }
        return true;
    }

    boolean validatePredicateConstructor(TypeMirror predicateMirror, Element variantElement) {
        TypeElement predicateElement = (TypeElement) processingEnv.getTypeUtils()
                .asElement(predicateMirror);
        if (predicateElement == null) {
            return true;
        }

        if (predicateElement.getModifiers().contains(Modifier.ABSTRACT)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Predicate class " + predicateElement.getSimpleName()
                            + " must not be abstract and must have an accessible no-arg constructor.",
                    variantElement
            );
            return false;
        }

        List<ExecutableElement> constructors = ElementFilter.constructorsIn(
                predicateElement.getEnclosedElements());

        boolean hasNoArgConstructor = constructors.isEmpty()
                || constructors.stream().anyMatch(c ->
                        c.getParameters().isEmpty()
                        && !c.getModifiers().contains(Modifier.PRIVATE));

        if (!hasNoArgConstructor) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Predicate class " + predicateElement.getSimpleName()
                            + " must have an accessible no-arg constructor.",
                    variantElement
            );
            return false;
        }
        return true;
    }

    private boolean checkMismatch(boolean hasCorrectAttr, String correctAttrName,
                                   boolean hasString, boolean hasAlt1, boolean hasAlt2, boolean hasAlt3,
                                   String variantName, String flagKey,
                                   FeatureType featureType, Element variantElement) {
        boolean hasWrongAttr = hasString || hasAlt1 || hasAlt2 || hasAlt3;
        if (hasWrongAttr && !hasCorrectAttr) {
            String wrongAttrDesc = describeWrongAttributes(hasString, hasAlt1, hasAlt2, hasAlt3, featureType);
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Type mismatch on variant " + variantName + " for feature \""
                            + flagKey + "\" (type " + featureType + "): "
                            + wrongAttrDesc + " value attribute used. "
                            + "Use " + correctAttrName + " instead.",
                    variantElement
            );
            return true;
        }
        return false;
    }

    private String describeWrongAttributes(boolean hasString, boolean hasAlt1, boolean hasAlt2,
                                            boolean hasAlt3, FeatureType featureType) {
        List<String> labels = WRONG_ATTR_LABELS.get(featureType);
        boolean[] flags = {hasString, hasAlt1, hasAlt2, hasAlt3};
        List<String> wrong = new ArrayList<>();
        for (int i = 0; i < flags.length; i++) {
            if (flags[i] && i < labels.size() && !labels.get(i).isEmpty()) {
                wrong.add(labels.get(i));
            }
        }
        return wrong.isEmpty() ? "wrong" : String.join("/", wrong);
    }

    private boolean rangesOverlap(VariantModel v1, VariantModel v2) {
        double distance = Math.abs(v1.doubleVariantValue() - v2.doubleVariantValue());
        double combinedDelta = v1.doubleDelta() + v2.doubleDelta();
        return distance < combinedDelta;
    }

    private String formatRange(VariantModel v) {
        double low = v.doubleVariantValue() - v.doubleDelta();
        double high = v.doubleVariantValue() + v.doubleDelta();
        return "[" + low + ", " + high + "]";
    }
}
