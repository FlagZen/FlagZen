package com.flagzen.processor;

import com.flagzen.CloseTo;
import com.flagzen.DefaultVariant;
import com.flagzen.FallbackStrategy;
import com.flagzen.Feature;
import com.flagzen.FeatureType;
import com.flagzen.Variant;
import com.flagzen.Variants;
import com.flagzen.WhenFalse;
import com.flagzen.WhenFalses;
import com.flagzen.WhenTrue;
import com.flagzen.WhenTrues;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that validates @Feature/@Variant annotations
 * and generates dispatch proxy classes and metadata via JavaPoet.
 */
public class FlagZenProcessor extends AbstractProcessor {

    private final ProxyGenerator proxyGenerator = new ProxyGenerator();
    private final List<String> metadataClassNames = new ArrayList<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(
                "com.flagzen.Feature",
                "com.flagzen.Variant",
                "com.flagzen.Variants",
                "com.flagzen.DefaultVariant",
                "com.flagzen.WhenTrue",
                "com.flagzen.WhenTrues",
                "com.flagzen.WhenFalse",
                "com.flagzen.WhenFalses"
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeServiceFile();
            return true;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Feature.class)) {
            processFeatureElement(element, roundEnv);
        }
        return true;
    }

    private void processFeatureElement(Element element, RoundEnvironment roundEnv) {
        if (element.getKind() != ElementKind.INTERFACE) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Feature can only be applied to interfaces",
                    element
            );
            return;
        }

        TypeElement featureElement = (TypeElement) element;
        Feature featureAnnotation = featureElement.getAnnotation(Feature.class);

        String packageName = getPackageName(featureElement);
        String interfaceName = featureElement.getSimpleName().toString();
        String flagKey = featureAnnotation.value();
        FallbackStrategy fallbackStrategy = featureAnnotation.fallback();
        FeatureType featureType = featureAnnotation.type();

        List<MethodModel> methods = extractMethods(featureElement);

        List<VariantModel> variants = collectVariants(roundEnv, featureElement, featureType);
        if (hasDuplicateVariantValues(variants, flagKey, featureElement)) {
            return;
        }
        String defaultVariantClassName = findDefaultVariant(roundEnv, featureElement);

        List<String> variantEnumValues = collectVariantEnumValues(featureElement);
        if (!variantEnumValues.isEmpty()) {
            validateVariantValuesAgainstEnum(variants, variantEnumValues, interfaceName, roundEnv);
        }

        if (fallbackStrategy == FallbackStrategy.REQUIRED
                && !variantEnumValues.isEmpty()
                && defaultVariantClassName == null) {
            if (hasIncompleteVariantCoverage(variants, variantEnumValues, flagKey, featureElement)) {
                return;
            }
        }

        if (fallbackStrategy == FallbackStrategy.REQUIRED
                && featureType == FeatureType.BOOLEAN
                && defaultVariantClassName == null) {
            if (hasIncompleteBooleanCoverage(variants, flagKey, featureElement)) {
                return;
            }
        }

        FeatureModel model = new FeatureModel(
                packageName, interfaceName, flagKey,
                fallbackStrategy, featureType, methods, variants, defaultVariantClassName
        );

        generateCode(model, featureElement);
    }

    private List<MethodModel> extractMethods(TypeElement featureElement) {
        List<MethodModel> methods = new ArrayList<>();
        for (Element enclosed : featureElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement methodElement = (ExecutableElement) enclosed;
            List<MethodModel.ParameterModel> params = new ArrayList<>();
            for (VariableElement param : methodElement.getParameters()) {
                params.add(new MethodModel.ParameterModel(
                        param.asType().toString(),
                        param.getSimpleName().toString()
                ));
            }
            methods.add(new MethodModel(
                    methodElement.getSimpleName().toString(),
                    methodElement.getReturnType().toString(),
                    params
            ));
        }
        return methods;
    }

    private void generateCode(FeatureModel model, TypeElement featureElement) {
        try {
            proxyGenerator.generateProxy(model)
                    .writeTo(processingEnv.getFiler());
            proxyGenerator.generateMetadata(model)
                    .writeTo(processingEnv.getFiler());
            metadataClassNames.add(model.packageName() + "." + model.metadataClassName());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to generate proxy: " + e.getMessage(),
                    featureElement
            );
        }
    }

    private void writeServiceFile() {
        if (metadataClassNames.isEmpty()) {
            return;
        }
        try {
            FileObject serviceFile = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/services/com.flagzen.spi.FeatureMetadata"
            );
            try (Writer writer = serviceFile.openWriter()) {
                for (String className : metadataClassNames) {
                    writer.write(className);
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "Failed to write service file: " + e.getMessage()
            );
        }
    }

    private String findDefaultVariant(RoundEnvironment roundEnv, TypeElement featureElement) {
        for (Element element : roundEnv.getElementsAnnotatedWith(DefaultVariant.class)) {
            DefaultVariant annotation = element.getAnnotation(DefaultVariant.class);
            TypeMirror targetFeature = extractDefaultVariantOf(annotation);
            if (targetFeature == null) {
                continue;
            }
            TypeElement targetElement = (TypeElement) processingEnv.getTypeUtils()
                    .asElement(targetFeature);
            if (targetElement == null || !targetElement.equals(featureElement)) {
                continue;
            }
            TypeElement defaultElement = (TypeElement) element;
            if (!implementsInterface(defaultElement, featureElement)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Default variant class " + defaultElement.getSimpleName()
                                + " must implement the feature interface "
                                + featureElement.getSimpleName(),
                        defaultElement
                );
                continue;
            }
            return defaultElement.getQualifiedName().toString();
        }
        return null;
    }

    private TypeMirror extractDefaultVariantOf(DefaultVariant annotation) {
        try {
            annotation.of();
            return null;
        } catch (MirroredTypeException e) {
            TypeMirror mirror = e.getTypeMirror();
            if (mirror.toString().equals("void")) {
                return null;
            }
            return mirror;
        }
    }

    private List<VariantModel> collectVariants(RoundEnvironment roundEnv, TypeElement featureElement,
                                                FeatureType featureType) {
        List<VariantModel> variants = new ArrayList<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Variant.class)) {
            processVariantAnnotation(element, element.getAnnotation(Variant.class), featureElement,
                    featureType, variants);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Variants.class)) {
            Variants container = element.getAnnotation(Variants.class);
            for (Variant variantAnnotation : container.value()) {
                processVariantAnnotation(element, variantAnnotation, featureElement,
                        featureType, variants);
            }
        }

        collectWhenTrueVariants(roundEnv, featureElement, variants);
        collectWhenFalseVariants(roundEnv, featureElement, variants);

        return variants;
    }

    private void collectWhenTrueVariants(RoundEnvironment roundEnv, TypeElement featureElement,
                                          List<VariantModel> variants) {
        for (Element element : roundEnv.getElementsAnnotatedWith(WhenTrue.class)) {
            TypeMirror target = extractWhenTrueOf(element.getAnnotation(WhenTrue.class));
            if (target == null) {
                target = inferFeatureTarget(element, featureElement);
            }
            processBooleanVariant(element, target, true, featureElement, variants);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(WhenTrues.class)) {
            for (WhenTrue annotation : element.getAnnotation(WhenTrues.class).value()) {
                TypeMirror target = extractWhenTrueOf(annotation);
                processBooleanVariant(element, target, true, featureElement, variants);
            }
        }
    }

    private void collectWhenFalseVariants(RoundEnvironment roundEnv, TypeElement featureElement,
                                           List<VariantModel> variants) {
        for (Element element : roundEnv.getElementsAnnotatedWith(WhenFalse.class)) {
            TypeMirror target = extractWhenFalseOf(element.getAnnotation(WhenFalse.class));
            if (target == null) {
                target = inferFeatureTarget(element, featureElement);
            }
            processBooleanVariant(element, target, false, featureElement, variants);
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(WhenFalses.class)) {
            for (WhenFalse annotation : element.getAnnotation(WhenFalses.class).value()) {
                TypeMirror target = extractWhenFalseOf(annotation);
                processBooleanVariant(element, target, false, featureElement, variants);
            }
        }
    }

    private TypeMirror inferFeatureTarget(Element element, TypeElement featureElement) {
        if (element.getKind() != ElementKind.CLASS) {
            return null;
        }
        TypeElement classElement = (TypeElement) element;
        for (TypeMirror iface : classElement.getInterfaces()) {
            TypeElement ifaceElement = (TypeElement) processingEnv.getTypeUtils().asElement(iface);
            if (ifaceElement != null && ifaceElement.equals(featureElement)) {
                return iface;
            }
        }
        return null;
    }

    private void processBooleanVariant(Element element, TypeMirror targetFeature, boolean booleanValue,
                                        TypeElement featureElement, List<VariantModel> variants) {
        if (targetFeature == null) {
            return;
        }
        TypeElement targetElement = (TypeElement) processingEnv.getTypeUtils()
                .asElement(targetFeature);
        if (targetElement == null || !targetElement.equals(featureElement)) {
            return;
        }
        TypeElement variantElement = (TypeElement) element;
        if (!implementsInterface(variantElement, featureElement)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Variant class " + variantElement.getSimpleName()
                            + " must implement the feature interface "
                            + featureElement.getSimpleName(),
                    variantElement
            );
            return;
        }
        variants.add(VariantModel.ofBoolean(variantElement.getQualifiedName().toString(), booleanValue));
    }

    private TypeMirror extractWhenTrueOf(WhenTrue annotation) {
        try {
            annotation.of();
            return null;
        } catch (MirroredTypeException e) {
            TypeMirror mirror = e.getTypeMirror();
            if (mirror.toString().equals("void")) {
                return null;
            }
            return mirror;
        }
    }

    private TypeMirror extractWhenFalseOf(WhenFalse annotation) {
        try {
            annotation.of();
            return null;
        } catch (MirroredTypeException e) {
            TypeMirror mirror = e.getTypeMirror();
            if (mirror.toString().equals("void")) {
                return null;
            }
            return mirror;
        }
    }

    private void processVariantAnnotation(Element element, Variant variantAnnotation,
                                          TypeElement featureElement, FeatureType featureType,
                                          List<VariantModel> variants) {
        TypeMirror targetFeature = extractVariantOf(variantAnnotation);
        if (targetFeature == null) {
            return;
        }
        TypeElement targetElement = (TypeElement) processingEnv.getTypeUtils()
                .asElement(targetFeature);
        if (targetElement == null || !targetElement.equals(featureElement)) {
            return;
        }
        TypeElement variantElement = (TypeElement) element;
        if (!implementsInterface(variantElement, featureElement)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Variant class " + variantElement.getSimpleName()
                            + " must implement the feature interface "
                            + featureElement.getSimpleName(),
                    variantElement
            );
            return;
        }

        String qualifiedName = variantElement.getQualifiedName().toString();
        String variantSimpleName = variantElement.getSimpleName().toString();
        Feature featureAnnotation = featureElement.getAnnotation(Feature.class);
        String flagKey = featureAnnotation.value();

        if (hasTypeMismatch(variantAnnotation, featureType, variantSimpleName, flagKey, variantElement)) {
            return;
        }

        if (featureType == FeatureType.INT) {
            variants.add(new VariantModel(qualifiedName, "", variantAnnotation.intValue(), featureType));
        } else if (featureType == FeatureType.LONG) {
            variants.add(VariantModel.ofLong(qualifiedName, variantAnnotation.longValue()));
        } else if (featureType == FeatureType.DOUBLE) {
            CloseTo[] closeToValues = variantAnnotation.doubleValue();
            if (closeToValues.length > 0) {
                CloseTo closeTo = closeToValues[0];
                variants.add(VariantModel.ofDouble(qualifiedName, closeTo.value(), closeTo.delta()));
            }
        } else if (featureType == FeatureType.BOOLEAN) {
            String boolStr = variantAnnotation.booleanValue();
            if (!boolStr.isEmpty()) {
                variants.add(VariantModel.ofBoolean(qualifiedName, Boolean.parseBoolean(boolStr)));
            }
        } else {
            for (String stringValue : variantAnnotation.value()) {
                variants.add(new VariantModel(qualifiedName, stringValue));
            }
        }
    }

    private boolean hasTypeMismatch(Variant annotation, FeatureType featureType,
                                     String variantName, String flagKey, Element variantElement) {
        boolean hasString = annotation.value().length > 0;
        boolean hasInt = annotation.intValue() != Integer.MIN_VALUE;
        boolean hasLong = annotation.longValue() != Long.MIN_VALUE;
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

    private boolean implementsInterface(TypeElement variantElement, TypeElement featureElement) {
        TypeMirror featureType = featureElement.asType();
        for (TypeMirror implementedInterface : variantElement.getInterfaces()) {
            if (processingEnv.getTypeUtils().isSameType(
                    processingEnv.getTypeUtils().erasure(implementedInterface),
                    processingEnv.getTypeUtils().erasure(featureType))) {
                return true;
            }
        }
        return false;
    }

    private TypeMirror extractVariantOf(Variant annotation) {
        try {
            annotation.of();
            return null; // default void.class
        } catch (MirroredTypeException e) {
            TypeMirror mirror = e.getTypeMirror();
            if (mirror.toString().equals("void")) {
                return null;
            }
            return mirror;
        }
    }

    private List<String> collectVariantEnumValues(TypeElement featureElement) {
        List<String> values = new ArrayList<>();
        for (Element enclosed : featureElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.ENUM
                    && enclosed.getSimpleName().toString().equals("Variant")) {
                TypeElement enumElement = (TypeElement) enclosed;
                for (Element enumConstant : enumElement.getEnclosedElements()) {
                    if (enumConstant.getKind() == ElementKind.ENUM_CONSTANT) {
                        values.add(enumConstant.getSimpleName().toString());
                    }
                }
            }
        }
        return values;
    }

    private void validateVariantValuesAgainstEnum(List<VariantModel> variants,
                                                   List<String> validValues,
                                                   String interfaceName,
                                                   RoundEnvironment roundEnv) {
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

    private boolean hasIncompleteVariantCoverage(List<VariantModel> variants,
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

    private String describeWrongAttributes(boolean hasString, boolean hasAlt1, boolean hasAlt2,
                                              boolean hasAlt3, FeatureType featureType) {
        List<String> wrong = new ArrayList<>();
        switch (featureType) {
            case INT -> {
                if (hasString) wrong.add("string");
                if (hasAlt1) wrong.add("long");
                if (hasAlt2) wrong.add("double");
                if (hasAlt3) wrong.add("boolean");
            }
            case LONG -> {
                if (hasString) wrong.add("string");
                if (hasAlt1) wrong.add("integer");
                if (hasAlt2) wrong.add("double");
                if (hasAlt3) wrong.add("boolean");
            }
            case DOUBLE -> {
                if (hasString) wrong.add("string");
                if (hasAlt1) wrong.add("integer");
                if (hasAlt2) wrong.add("long");
                if (hasAlt3) wrong.add("boolean");
            }
            case BOOLEAN -> {
                if (hasString) wrong.add("string");
                if (hasAlt1) wrong.add("integer");
                if (hasAlt2) wrong.add("long");
                if (hasAlt3) wrong.add("double");
            }
            case STRING -> {
                if (hasAlt1) wrong.add("integer");
                if (hasAlt2) wrong.add("long");
                if (hasAlt3) wrong.add("double");
            }
        }
        return wrong.isEmpty() ? "wrong" : String.join("/", wrong);
    }

    private boolean hasIncompleteBooleanCoverage(List<VariantModel> variants, String flagKey,
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

    private boolean hasDuplicateVariantValues(List<VariantModel> variants, String flagKey,
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

    private String getPackageName(TypeElement element) {
        Element enclosing = element.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return enclosing != null ? ((PackageElement) enclosing).getQualifiedName().toString() : "";
    }
}
