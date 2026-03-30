package com.flagzen.processor;

import com.flagzen.CloseTo;
import com.flagzen.Condition;
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
import javax.annotation.processing.ProcessingEnvironment;
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
import java.util.List;
import java.util.Set;

/**
 * Annotation processor that validates @Feature/@Variant annotations
 * and generates dispatch proxy classes and metadata via JavaPoet.
 */
public class FlagZenProcessor extends AbstractProcessor {

    private final ProxyGenerator proxyGenerator = new ProxyGenerator();
    private final MetadataGenerator metadataGenerator = new MetadataGenerator();
    private final List<String> metadataClassNames = new ArrayList<>();
    private VariantValidator validator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.validator = new VariantValidator(processingEnv);
    }

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
        if (validator.hasDuplicateVariantValues(variants, flagKey, featureElement)) {
            return;
        }
        if (featureType == FeatureType.DOUBLE && validator.hasOverlappingCloseToRanges(variants, featureElement)) {
            return;
        }
        String defaultVariantClassName = findDefaultVariant(roundEnv, featureElement);

        List<String> variantEnumValues = collectVariantEnumValues(featureElement);
        if (!variantEnumValues.isEmpty()) {
            validator.validateVariantValuesAgainstEnum(variants, variantEnumValues, interfaceName);
        }

        if (validator.hasDuplicateOrderValues(variants, flagKey, featureElement)) {
            return;
        }

        boolean hasConditions = variants.stream().anyMatch(v -> v.condition() != null);
        if (fallbackStrategy == FallbackStrategy.REQUIRED
                && hasConditions
                && defaultVariantClassName == null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Feature \"" + flagKey + "\" uses REQUIRED fallback with condition-based variants "
                            + "but has no @DefaultVariant. Add a @DefaultVariant to ensure a fallback "
                            + "when no condition matches.",
                    featureElement
            );
            return;
        }

        if (fallbackStrategy == FallbackStrategy.REQUIRED
                && !variantEnumValues.isEmpty()
                && defaultVariantClassName == null) {
            if (validator.hasIncompleteVariantCoverage(variants, variantEnumValues, flagKey, featureElement)) {
                return;
            }
        }

        if (fallbackStrategy == FallbackStrategy.REQUIRED
                && featureType == FeatureType.BOOLEAN
                && defaultVariantClassName == null) {
            if (validator.hasIncompleteBooleanCoverage(variants, flagKey, featureElement)) {
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
            metadataGenerator.generateMetadata(model)
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

        if (validator.hasTypeMismatch(variantAnnotation, featureType, variantSimpleName, flagKey, variantElement)) {
            return;
        }

        ConditionModel conditionModel = extractConditionModel(variantAnnotation, featureType, variantElement);
        int order = variantAnnotation.order();

        // Condition-only variant: no typed value, just a predicate
        if (conditionModel != null && !hasTypedValue(variantAnnotation)) {
            variants.add(new VariantModel(qualifiedName, "", Integer.MIN_VALUE, Long.MIN_VALUE,
                    Double.NaN, 0.0, false, featureType, conditionModel, order));
            return;
        }

        if (featureType == FeatureType.INT) {
            for (int intVal : variantAnnotation.intValue()) {
                variants.add(new VariantModel(qualifiedName, "", intVal, Long.MIN_VALUE,
                        Double.NaN, 0.0, false, featureType, conditionModel, order));
            }
        } else if (featureType == FeatureType.LONG) {
            for (long longVal : variantAnnotation.longValue()) {
                variants.add(new VariantModel(qualifiedName, "", Integer.MIN_VALUE, longVal,
                        Double.NaN, 0.0, false, FeatureType.LONG, conditionModel, order));
            }
        } else if (featureType == FeatureType.DOUBLE) {
            for (CloseTo closeTo : variantAnnotation.doubleValue()) {
                variants.add(new VariantModel(qualifiedName, "", Integer.MIN_VALUE, Long.MIN_VALUE,
                        closeTo.value(), closeTo.delta(), false, FeatureType.DOUBLE, conditionModel, order));
            }
        } else if (featureType == FeatureType.BOOLEAN) {
            String boolStr = variantAnnotation.booleanValue();
            if (!boolStr.isEmpty()) {
                variants.add(new VariantModel(qualifiedName, "", Integer.MIN_VALUE, Long.MIN_VALUE,
                        Double.NaN, 0.0, Boolean.parseBoolean(boolStr), FeatureType.BOOLEAN,
                        conditionModel, order));
            }
        } else {
            for (String stringValue : variantAnnotation.value()) {
                if (stringValue.isEmpty()) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "@Variant on " + variantSimpleName + ": empty variant values are not permitted",
                            variantElement
                    );
                    return;
                }
                variants.add(new VariantModel(qualifiedName, stringValue, conditionModel, order));
            }
        }
    }

    private ConditionModel extractConditionModel(Variant variantAnnotation,
                                                   FeatureType featureType,
                                                   Element variantElement) {
        Condition condition = variantAnnotation.when();

        TypeMirror matchesMirror = extractConditionMatches(condition);
        TypeMirror notMatchesMirror = extractConditionNotMatches(condition);

        boolean hasMatches = matchesMirror != null
                && !matchesMirror.toString().equals(Condition.Sentinel.class.getCanonicalName());
        boolean hasNotMatches = notMatchesMirror != null
                && !notMatchesMirror.toString().equals(Condition.Sentinel.class.getCanonicalName());

        if (hasMatches && hasNotMatches) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "@Condition: matches and notMatches are mutually exclusive. "
                            + "Use only one on each @Condition.",
                    variantElement
            );
            return null;
        }

        TypeMirror predicateMirror = hasMatches ? matchesMirror : hasNotMatches ? notMatchesMirror : null;
        if (predicateMirror != null) {
            if (!validator.validatePredicateType(predicateMirror, featureType, variantElement)) {
                return null;
            }
            if (!validator.validatePredicateConstructor(predicateMirror, variantElement)) {
                return null;
            }
        }

        if (hasMatches) {
            return new ConditionModel(matchesMirror.toString(), false);
        }
        if (hasNotMatches) {
            return new ConditionModel(notMatchesMirror.toString(), true);
        }
        return null;
    }

    private TypeMirror extractConditionMatches(Condition condition) {
        try {
            condition.matches();
            return null;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    private TypeMirror extractConditionNotMatches(Condition condition) {
        try {
            condition.notMatches();
            return null;
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    private boolean hasTypedValue(Variant annotation) {
        return annotation.value().length > 0
                || annotation.intValue().length > 0
                || annotation.longValue().length > 0
                || annotation.doubleValue().length > 0
                || !annotation.booleanValue().isEmpty();
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

    private String getPackageName(TypeElement element) {
        Element enclosing = element.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return enclosing != null ? ((PackageElement) enclosing).getQualifiedName().toString() : "";
    }
}
