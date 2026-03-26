package com.flagzen.processor;

import com.flagzen.DefaultVariant;
import com.flagzen.FallbackStrategy;
import com.flagzen.Feature;
import com.flagzen.Variant;
import com.flagzen.Variants;

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
                "com.flagzen.DefaultVariant"
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
            if (element.getKind() != ElementKind.INTERFACE) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@Feature can only be applied to interfaces",
                        element
                );
                continue;
            }

            TypeElement featureElement = (TypeElement) element;
            Feature featureAnnotation = featureElement.getAnnotation(Feature.class);

            String packageName = getPackageName(featureElement);
            String interfaceName = featureElement.getSimpleName().toString();
            String flagKey = featureAnnotation.value();
            FallbackStrategy fallbackStrategy = featureAnnotation.fallback();

            List<MethodModel> methods = new ArrayList<>();
            for (Element enclosed : featureElement.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.METHOD) {
                    ExecutableElement methodElement = (ExecutableElement) enclosed;
                    String methodName = methodElement.getSimpleName().toString();
                    String returnType = methodElement.getReturnType().toString();

                    List<MethodModel.ParameterModel> params = new ArrayList<>();
                    for (VariableElement param : methodElement.getParameters()) {
                        params.add(new MethodModel.ParameterModel(
                                param.asType().toString(),
                                param.getSimpleName().toString()
                        ));
                    }
                    methods.add(new MethodModel(methodName, returnType, params));
                }
            }

            List<VariantModel> variants = collectVariants(roundEnv, featureElement);
            if (hasDuplicateVariantValues(variants, flagKey, featureElement)) {
                continue;
            }
            String defaultVariantClassName = findDefaultVariant(roundEnv, featureElement);

            List<String> variantEnumValues = collectVariantEnumValues(featureElement);
            if (!variantEnumValues.isEmpty()) {
                validateVariantValuesAgainstEnum(variants, variantEnumValues, interfaceName, roundEnv);
            }

            FeatureModel model = new FeatureModel(
                    packageName, interfaceName, flagKey,
                    fallbackStrategy, methods, variants, defaultVariantClassName
            );

            try {
                proxyGenerator.generateProxy(model)
                        .writeTo(processingEnv.getFiler());
                proxyGenerator.generateMetadata(model)
                        .writeTo(processingEnv.getFiler());
                metadataClassNames.add(packageName + "." + model.metadataClassName());
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate proxy: " + e.getMessage(),
                        featureElement
                );
            }
        }
        return true;
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

    private List<VariantModel> collectVariants(RoundEnvironment roundEnv, TypeElement featureElement) {
        List<VariantModel> variants = new ArrayList<>();

        for (Element element : roundEnv.getElementsAnnotatedWith(Variant.class)) {
            processVariantAnnotation(element, element.getAnnotation(Variant.class), featureElement, variants);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Variants.class)) {
            Variants container = element.getAnnotation(Variants.class);
            for (Variant variantAnnotation : container.value()) {
                processVariantAnnotation(element, variantAnnotation, featureElement, variants);
            }
        }

        return variants;
    }

    private void processVariantAnnotation(Element element, Variant variantAnnotation,
                                          TypeElement featureElement, List<VariantModel> variants) {
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
        variants.add(new VariantModel(qualifiedName, variantAnnotation.value()));
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

    private boolean hasDuplicateVariantValues(List<VariantModel> variants, String flagKey,
                                                TypeElement featureElement) {
        Map<String, List<String>> valueToClassNames = new HashMap<>();
        for (VariantModel variant : variants) {
            String simpleName = variant.qualifiedClassName()
                    .substring(variant.qualifiedClassName().lastIndexOf('.') + 1);
            valueToClassNames
                    .computeIfAbsent(variant.variantValue(), k -> new ArrayList<>())
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
