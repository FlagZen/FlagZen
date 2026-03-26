package com.flagzen.processor;

import com.flagzen.FallbackStrategy;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.FieldSpec;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generates {Feature}_FlagZenProxy classes using JavaPoet.
 */
final class ProxyGenerator {

    JavaFile generate(FeatureModel model) {
        ClassName interfaceType = ClassName.get(model.packageName(), model.interfaceName());
        ClassName supplierOfFeature = ClassName.get(Supplier.class);
        ParameterizedTypeName supplierType = ParameterizedTypeName.get(supplierOfFeature, interfaceType);
        ParameterizedTypeName variantsMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                supplierType
        );

        TypeSpec.Builder proxyBuilder = TypeSpec.classBuilder(model.proxyClassName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceType);

        // Fields
        proxyBuilder.addField(FieldSpec.builder(String.class, "flagKey", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(variantsMapType, "variants", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(supplierType, "defaultVariant", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(FallbackStrategy.class, "fallbackStrategy", Modifier.PRIVATE, Modifier.FINAL).build());

        // Package-private constructor
        proxyBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(String.class, "flagKey")
                .addParameter(variantsMapType, "variants")
                .addParameter(supplierType, "defaultVariant")
                .addParameter(FallbackStrategy.class, "fallbackStrategy")
                .addStatement("this.flagKey = flagKey")
                .addStatement("this.variants = variants")
                .addStatement("this.defaultVariant = defaultVariant")
                .addStatement("this.fallbackStrategy = fallbackStrategy")
                .build());

        // Interface methods — stub for walking skeleton
        for (String methodName : model.methodNames()) {
            proxyBuilder.addMethod(MethodSpec.methodBuilder(methodName)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("throw new $T($S)",
                            UnsupportedOperationException.class,
                            "Runtime dispatch not yet wired — see step 01-02")
                    .build());
        }

        // toString
        proxyBuilder.addMethod(MethodSpec.methodBuilder("toString")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", "FlagZenProxy[" + model.flagKey() + "]")
                .build());

        TypeSpec proxyClass = proxyBuilder.build();
        return JavaFile.builder(model.packageName(), proxyClass).build();
    }
}
