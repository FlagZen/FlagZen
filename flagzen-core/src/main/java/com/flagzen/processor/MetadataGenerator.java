package com.flagzen.processor;

import com.flagzen.FallbackStrategy;
import com.flagzen.FeatureType;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generates {Feature}_FlagZenMetadata classes using JavaPoet.
 */
final class MetadataGenerator {

    JavaFile generateMetadata(FeatureModel model) {
        ClassName interfaceType = ClassName.get(model.packageName(), model.interfaceName());
        ClassName proxyType = ClassName.get(model.packageName(), model.proxyClassName());
        ClassName flagProviderType = ClassName.get(FlagProvider.class);
        ClassName supplierOfFeature = ClassName.get(Supplier.class);
        ParameterizedTypeName supplierType = ParameterizedTypeName.get(supplierOfFeature, interfaceType);
        // Metadata always uses String keys (SPI contract)
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
        metadataBuilder.addMethod(buildVariantSuppliersMethod(model, supplierType, variantsMapType));

        // defaultVariantSupplier()
        metadataBuilder.addMethod(buildDefaultVariantSupplierMethod(model, supplierType));

        // createProxy()
        metadataBuilder.addMethod(buildCreateProxyMethod(model, interfaceType, proxyType,
                flagProviderType, variantsMapType, supplierType));

        TypeSpec metadataClass = metadataBuilder.build();
        return JavaFile.builder(model.packageName(), metadataClass).build();
    }

    private MethodSpec buildVariantSuppliersMethod(FeatureModel model,
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
                // Metadata always uses string keys (SPI contract)
                mapBuilder.add("\n    $S, ($T) $T::new",
                        variant.variantKeyLiteral(),
                        supplierType,
                        ClassName.bestGuess(variant.qualifiedClassName()));
            }
            mapBuilder.add(")");
            builder.addStatement(mapBuilder.build());
        }

        return builder.build();
    }

    private MethodSpec buildCreateProxyMethod(FeatureModel model, ClassName interfaceType,
                                                ClassName proxyType, ClassName flagProviderType,
                                                ParameterizedTypeName variantsMapType,
                                                ParameterizedTypeName supplierType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createProxy")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(interfaceType)
                .addParameter(flagProviderType, "flagProvider")
                .addParameter(variantsMapType, "variants")
                .addParameter(supplierType, "defaultVariant");

        if (model.hasOrderedDispatch()) {
            // Ordered dispatch proxy: constructor takes (flagProvider, defaultVariant, fallbackStrategy)
            builder.addStatement("return new $T(flagProvider, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        } else if (model.featureType() == FeatureType.INT) {
            buildTypedMapConversion(builder, proxyType, supplierType,
                    Integer.class, "parseInt", model);
        } else if (model.featureType() == FeatureType.LONG) {
            buildTypedMapConversion(builder, proxyType, supplierType,
                    Long.class, "parseLong", model);
        } else if (model.featureType() == FeatureType.DOUBLE) {
            buildTypedMapConversion(builder, proxyType, supplierType,
                    Double.class, "parseDouble", model);
        } else if (model.featureType() == FeatureType.BOOLEAN) {
            buildTypedMapConversion(builder, proxyType, supplierType,
                    Boolean.class, "parseBoolean", model);
        } else {
            builder.addStatement("return new $T(flagProvider, variants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        }

        return builder.build();
    }

    private void buildTypedMapConversion(MethodSpec.Builder builder, ClassName proxyType,
                                          ParameterizedTypeName supplierType,
                                          Class<?> boxedType, String parseMethod,
                                          FeatureModel model) {
        ParameterizedTypeName typedMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(boxedType),
                supplierType
        );
        String varName = boxedType.getSimpleName().toLowerCase() + "Variants";
        builder.addStatement("$T $L = new $T<>()", typedMapType, varName, ClassName.get("java.util", "HashMap"));
        builder.addStatement("variants.forEach((k, v) -> $L.put($T.$L(k), v))",
                varName, boxedType, parseMethod);
        builder.addStatement("return new $T(flagProvider, $L, defaultVariant, $T.$L)",
                proxyType, varName, FallbackStrategy.class, model.fallbackStrategy().name());
    }

    private MethodSpec buildDefaultVariantSupplierMethod(FeatureModel model,
                                                          ParameterizedTypeName supplierType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("defaultVariantSupplier")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(supplierType);

        if (model.defaultVariantClassName() != null) {
            builder.addStatement("return ($T) $T::new",
                    supplierType,
                    ClassName.bestGuess(model.defaultVariantClassName()));
        } else {
            builder.addStatement("return null");
        }

        return builder.build();
    }
}
