package com.flagzen.processor;

import com.flagzen.FallbackStrategy;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generates {Feature}_FlagZenProxy and {Feature}_FlagZenMetadata classes using JavaPoet.
 */
final class ProxyGenerator {

    JavaFile generateProxy(FeatureModel model) {
        ClassName interfaceType = ClassName.get(model.packageName(), model.interfaceName());
        ClassName flagProviderType = ClassName.get(FlagProvider.class);
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
        proxyBuilder.addField(FieldSpec.builder(flagProviderType, "flagProvider", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(variantsMapType, "variants", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(supplierType, "defaultVariant", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(FallbackStrategy.class, "fallbackStrategy", Modifier.PRIVATE, Modifier.FINAL).build());

        // Package-private constructor
        proxyBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(flagProviderType, "flagProvider")
                .addParameter(variantsMapType, "variants")
                .addParameter(supplierType, "defaultVariant")
                .addParameter(FallbackStrategy.class, "fallbackStrategy")
                .addStatement("this.flagProvider = flagProvider")
                .addStatement("this.variants = variants")
                .addStatement("this.defaultVariant = defaultVariant")
                .addStatement("this.fallbackStrategy = fallbackStrategy")
                .build());

        // Private resolve method
        proxyBuilder.addMethod(buildResolveMethod(model, interfaceType, supplierType));

        // Interface methods with dispatch logic
        for (MethodModel method : model.methods()) {
            proxyBuilder.addMethod(buildDispatchMethod(method, interfaceType));
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

    JavaFile generateMetadata(FeatureModel model) {
        ClassName interfaceType = ClassName.get(model.packageName(), model.interfaceName());
        ClassName proxyType = ClassName.get(model.packageName(), model.proxyClassName());
        ClassName flagProviderType = ClassName.get(FlagProvider.class);
        ClassName supplierOfFeature = ClassName.get(Supplier.class);
        ParameterizedTypeName supplierType = ParameterizedTypeName.get(supplierOfFeature, interfaceType);
        ParameterizedTypeName variantsMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                supplierType
        );
        ParameterizedTypeName metadataType = ParameterizedTypeName.get(
                ClassName.get(FeatureMetadata.class),
                interfaceType
        );

        TypeSpec.Builder metadataBuilder = TypeSpec.classBuilder(model.metadataClassName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(metadataType);

        // featureType()
        metadataBuilder.addMethod(MethodSpec.methodBuilder("featureType")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), interfaceType))
                .addStatement("return $T.class", interfaceType)
                .build());

        // flagKey()
        metadataBuilder.addMethod(MethodSpec.methodBuilder("flagKey")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", model.flagKey())
                .build());

        // fallbackStrategy()
        metadataBuilder.addMethod(MethodSpec.methodBuilder("fallbackStrategy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(FallbackStrategy.class)
                .addStatement("return $T.$L", FallbackStrategy.class, model.fallbackStrategy().name())
                .build());

        // variantSuppliers()
        metadataBuilder.addMethod(buildVariantSuppliersMethod(model, interfaceType, supplierType, variantsMapType));

        // defaultVariantSupplier()
        metadataBuilder.addMethod(buildDefaultVariantSupplierMethod(model, interfaceType, supplierType));

        // createProxy()
        metadataBuilder.addMethod(MethodSpec.methodBuilder("createProxy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceType)
                .addParameter(flagProviderType, "flagProvider")
                .addParameter(variantsMapType, "variants")
                .addParameter(supplierType, "defaultVariant")
                .addStatement("return new $T(flagProvider, variants, defaultVariant, $T.$L)",
                        proxyType, FallbackStrategy.class, model.fallbackStrategy().name())
                .build());

        TypeSpec metadataClass = metadataBuilder.build();
        return JavaFile.builder(model.packageName(), metadataClass).build();
    }

    private MethodSpec buildResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        return MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T<$T> flagValue = flagProvider.getString($S)",
                        Optional.class, String.class, model.flagKey())
                .addStatement("$T value = flagValue.orElse(null)", String.class)
                .beginControlFlow("if (value != null)")
                .addStatement("$T supplier = variants.get(value)", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow()
                .addStatement("throw new $T($S, $T.valueOf(value))",
                        UnmatchedVariantException.class, model.flagKey(), String.class)
                .build();
    }

    private MethodSpec buildDispatchMethod(MethodModel method, ClassName interfaceType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        // Add parameters
        for (MethodModel.ParameterModel param : method.parameters()) {
            builder.addParameter(ClassName.bestGuess(param.type()), param.name());
        }

        // Build parameter names for delegation
        StringBuilder paramNames = new StringBuilder();
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) paramNames.append(", ");
            paramNames.append(method.parameters().get(i).name());
        }

        if (method.isVoid()) {
            builder.addStatement("resolveVariant().$L($L)", method.name(), paramNames);
        } else {
            builder.returns(ClassName.bestGuess(method.returnType()));
            builder.addStatement("return resolveVariant().$L($L)", method.name(), paramNames);
        }

        return builder.build();
    }

    private MethodSpec buildVariantSuppliersMethod(FeatureModel model, ClassName interfaceType,
                                                    ParameterizedTypeName supplierType,
                                                    ParameterizedTypeName variantsMapType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("variantSuppliers")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(variantsMapType);

        if (model.variants().isEmpty()) {
            builder.addStatement("return $T.of()", Map.class);
        } else {
            CodeBlock.Builder mapBuilder = CodeBlock.builder().add("return $T.of(", Map.class);
            for (int i = 0; i < model.variants().size(); i++) {
                VariantModel variant = model.variants().get(i);
                if (i > 0) mapBuilder.add(",");
                mapBuilder.add("\n    $S, ($T) $T::new",
                        variant.variantValue(),
                        supplierType,
                        ClassName.bestGuess(variant.qualifiedClassName()));
            }
            mapBuilder.add(")");
            builder.addStatement(mapBuilder.build());
        }

        return builder.build();
    }

    private MethodSpec buildDefaultVariantSupplierMethod(FeatureModel model, ClassName interfaceType,
                                                          ParameterizedTypeName supplierType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("defaultVariantSupplier")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(supplierType);

        // For now, no @DefaultVariant detection — return null
        // TODO: enhance when @DefaultVariant support is wired in the processor
        builder.addStatement("return null");

        return builder.build();
    }

    /**
     * @deprecated Use {@link #generateProxy(FeatureModel)} instead.
     */
    @Deprecated
    JavaFile generate(FeatureModel model) {
        return generateProxy(model);
    }
}
