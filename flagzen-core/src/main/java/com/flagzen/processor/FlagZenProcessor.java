package com.flagzen.processor;

import com.flagzen.FallbackStrategy;
import com.flagzen.Feature;
import com.flagzen.Variant;

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
import java.util.List;
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
                "com.flagzen.Variant"
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

            FeatureModel model = new FeatureModel(
                    packageName, interfaceName, flagKey,
                    fallbackStrategy, methods, variants
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

    private List<VariantModel> collectVariants(RoundEnvironment roundEnv, TypeElement featureElement) {
        List<VariantModel> variants = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Variant.class)) {
            Variant variantAnnotation = element.getAnnotation(Variant.class);
            TypeMirror targetFeature = extractVariantOf(variantAnnotation);

            if (targetFeature != null) {
                TypeElement targetElement = (TypeElement) processingEnv.getTypeUtils()
                        .asElement(targetFeature);
                if (targetElement != null && targetElement.equals(featureElement)) {
                    String qualifiedName = ((TypeElement) element).getQualifiedName().toString();
                    variants.add(new VariantModel(qualifiedName, variantAnnotation.value()));
                }
            }
        }
        return variants;
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

    private String getPackageName(TypeElement element) {
        Element enclosing = element.getEnclosingElement();
        while (enclosing != null && !(enclosing instanceof PackageElement)) {
            enclosing = enclosing.getEnclosingElement();
        }
        return enclosing != null ? ((PackageElement) enclosing).getQualifiedName().toString() : "";
    }
}
