package com.flagzen.processor;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
import com.flagzen.FeatureType;
import com.flagzen.FlagContext;
import com.flagzen.UnmatchedVariantException;
import com.flagzen.spi.FeatureMetadata;
import com.flagzen.spi.FlagProvider;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Generates {Feature}_FlagZenProxy and {Feature}_FlagZenMetadata classes using JavaPoet.
 */
final class ProxyGenerator {

    JavaFile generateProxy(FeatureModel model) {
        ClassName interfaceType = ClassName.get(model.packageName(), model.interfaceName());
        ClassName flagProviderType = ClassName.get(FlagProvider.class);
        ClassName supplierOfFeature = ClassName.get(Supplier.class);
        ParameterizedTypeName supplierType = ParameterizedTypeName.get(supplierOfFeature, interfaceType);

        TypeSpec.Builder proxyBuilder = TypeSpec.classBuilder(model.proxyClassName())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceType);

        if (model.hasOrderedDispatch()) {
            buildOrderedDispatchProxy(proxyBuilder, model, interfaceType, flagProviderType, supplierType);
        } else {
            buildMapBasedProxy(proxyBuilder, model, interfaceType, flagProviderType, supplierType);
        }

        // Private resolve method
        proxyBuilder.addMethod(buildResolveMethod(model, interfaceType, supplierType));

        // Interface methods with dispatch logic
        for (MethodModel method : model.methods()) {
            proxyBuilder.addMethod(buildDispatchMethod(method, interfaceType, model.fallbackStrategy()));
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

    private void buildMapBasedProxy(TypeSpec.Builder proxyBuilder, FeatureModel model,
                                     ClassName interfaceType, ClassName flagProviderType,
                                     ParameterizedTypeName supplierType) {
        TypeName variantKeyType = variantKeyType(model.featureType());
        ParameterizedTypeName variantsMapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                variantKeyType,
                supplierType
        );

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
    }

    private void buildOrderedDispatchProxy(TypeSpec.Builder proxyBuilder, FeatureModel model,
                                            ClassName interfaceType, ClassName flagProviderType,
                                            ParameterizedTypeName supplierType) {
        TypeName predicateType = predicateTypeForFeature(model.featureType());

        // Fields: flagProvider, defaultVariant, fallbackStrategy (common)
        proxyBuilder.addField(FieldSpec.builder(flagProviderType, "flagProvider", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(supplierType, "defaultVariant", Modifier.PRIVATE, Modifier.FINAL).build());
        proxyBuilder.addField(FieldSpec.builder(FallbackStrategy.class, "fallbackStrategy", Modifier.PRIVATE, Modifier.FINAL).build());

        // Sort variants by order for deterministic field naming and dispatch sequence
        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        // Generate predicate fields and variant supplier fields
        MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder()
                .addParameter(flagProviderType, "flagProvider")
                .addParameter(supplierType, "defaultVariant")
                .addParameter(FallbackStrategy.class, "fallbackStrategy")
                .addStatement("this.flagProvider = flagProvider")
                .addStatement("this.defaultVariant = defaultVariant")
                .addStatement("this.fallbackStrategy = fallbackStrategy");

        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);

            // Variant supplier field
            String supplierFieldName = "variant" + i;
            proxyBuilder.addField(FieldSpec.builder(supplierType, supplierFieldName, Modifier.PRIVATE, Modifier.FINAL).build());
            ctorBuilder.addStatement("this.$L = ($T) $T::new",
                    supplierFieldName, supplierType,
                    ClassName.bestGuess(variant.qualifiedClassName()));

            // Predicate field (only for condition-based variants)
            if (variant.condition() != null) {
                String predFieldName = "pred" + i;
                proxyBuilder.addField(FieldSpec.builder(predicateType, predFieldName, Modifier.PRIVATE, Modifier.FINAL).build());
                ctorBuilder.addStatement("this.$L = new $T()",
                        predFieldName,
                        ClassName.bestGuess(variant.condition().predicateClassName()));
            }
        }

        proxyBuilder.addMethod(ctorBuilder.build());
    }

    private static TypeName predicateTypeForFeature(FeatureType featureType) {
        return switch (featureType) {
            case INT -> ClassName.get(IntPredicate.class);
            case LONG -> ClassName.get(LongPredicate.class);
            case DOUBLE -> ClassName.get(DoublePredicate.class);
            case BOOLEAN -> ParameterizedTypeName.get(
                    ClassName.get(Predicate.class), ClassName.get(Boolean.class));
            default -> ParameterizedTypeName.get(
                    ClassName.get(Predicate.class), ClassName.get(String.class));
        };
    }

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
        metadataBuilder.addMethod(buildVariantSuppliersMethod(model, interfaceType, supplierType, variantsMapType));

        // defaultVariantSupplier()
        metadataBuilder.addMethod(buildDefaultVariantSupplierMethod(model, interfaceType, supplierType));

        // createProxy()
        metadataBuilder.addMethod(buildCreateProxyMethod(model, interfaceType, proxyType,
                flagProviderType, variantsMapType, supplierType));

        TypeSpec metadataClass = metadataBuilder.build();
        return JavaFile.builder(model.packageName(), metadataClass).build();
    }

    private MethodSpec buildResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        return switch (model.featureType()) {
            case INT -> buildIntResolveMethod(model, interfaceType, supplierType);
            case LONG -> buildLongResolveMethod(model, interfaceType, supplierType);
            case BOOLEAN -> buildBooleanResolveMethod(model, interfaceType, supplierType);
            case DOUBLE -> buildDoubleResolveMethod(model, interfaceType, supplierType);
            default -> buildStringResolveMethod(model, interfaceType, supplierType);
        };
    }

    private MethodSpec buildIntResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        if (model.hasOrderedDispatch()) {
            return buildOrderedIntResolveMethod(model, interfaceType);
        }
        return buildMapBasedIntResolveMethod(model, interfaceType, supplierType);
    }

    private MethodSpec buildLongResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        if (model.hasOrderedDispatch()) {
            return buildOrderedLongResolveMethod(model, interfaceType);
        }
        return buildMapBasedLongResolveMethod(model, interfaceType, supplierType);
    }

    private MethodSpec buildDoubleResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        if (model.hasOrderedDispatch()) {
            return buildOrderedDoubleResolveMethod(model, interfaceType);
        }
        return buildMapBasedDoubleResolveMethod(model, interfaceType, supplierType);
    }

    private MethodSpec buildBooleanResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        if (model.hasOrderedDispatch()) {
            return buildOrderedBooleanResolveMethod(model, interfaceType);
        }
        return buildMapBasedBooleanResolveMethod(model, interfaceType, supplierType);
    }

    private MethodSpec buildMapBasedIntResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        ClassName optionalIntType = ClassName.get("java.util", "OptionalInt");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getInt($S, context) : flagProvider.getInt($S)",
                        optionalIntType, model.flagKey(), model.flagKey());

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("$T supplier = variants.get(flagValue.getAsInt())", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsInt()), variants.keySet())",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildOrderedIntResolveMethod(FeatureModel model, ClassName interfaceType) {
        ClassName optionalIntType = ClassName.get("java.util", "OptionalInt");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getInt($S, context) : flagProvider.getInt($S)",
                        optionalIntType, model.flagKey(), model.flagKey());

        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("int value = flagValue.getAsInt()");

        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);
            String supplierFieldName = "variant" + i;

            if (variant.condition() != null) {
                String predFieldName = "pred" + i;
                if (variant.condition().negated()) {
                    builder.beginControlFlow("if (!$L.test(value))", predFieldName);
                } else {
                    builder.beginControlFlow("if ($L.test(value))", predFieldName);
                }
            } else {
                builder.beginControlFlow("if (value == $L)", variant.intVariantValue());
            }

            builder.addStatement("return $L.get()", supplierFieldName);
            builder.endControlFlow();
        }

        builder.endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsInt()))",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildMapBasedLongResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        ClassName optionalLongType = ClassName.get("java.util", "OptionalLong");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getLong($S, context) : flagProvider.getLong($S)",
                        optionalLongType, model.flagKey(), model.flagKey());

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("$T supplier = variants.get(flagValue.getAsLong())", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsLong()), variants.keySet())",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildOrderedLongResolveMethod(FeatureModel model, ClassName interfaceType) {
        ClassName optionalLongType = ClassName.get("java.util", "OptionalLong");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getLong($S, context) : flagProvider.getLong($S)",
                        optionalLongType, model.flagKey(), model.flagKey());

        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("long value = flagValue.getAsLong()");

        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);
            String supplierFieldName = "variant" + i;

            if (variant.condition() != null) {
                String predFieldName = "pred" + i;
                if (variant.condition().negated()) {
                    builder.beginControlFlow("if (!$L.test(value))", predFieldName);
                } else {
                    builder.beginControlFlow("if ($L.test(value))", predFieldName);
                }
            } else {
                builder.beginControlFlow("if (value == $LL)", variant.longVariantValue());
            }

            builder.addStatement("return $L.get()", supplierFieldName);
            builder.endControlFlow();
        }

        builder.endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsLong()))",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildMapBasedDoubleResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        ClassName optionalDoubleType = ClassName.get("java.util", "OptionalDouble");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getDouble($S, context) : flagProvider.getDouble($S)",
                        optionalDoubleType, model.flagKey(), model.flagKey());

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("double rawValue = flagValue.getAsDouble()")
                .addStatement("$T supplier = variants.get(rawValue)", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsDouble()), variants.keySet())",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildOrderedDoubleResolveMethod(FeatureModel model, ClassName interfaceType) {
        ClassName optionalDoubleType = ClassName.get("java.util", "OptionalDouble");
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T flagValue = (context != null) ? flagProvider.getDouble($S, context) : flagProvider.getDouble($S)",
                        optionalDoubleType, model.flagKey(), model.flagKey());

        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("double value = flagValue.getAsDouble()");

        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);
            String supplierFieldName = "variant" + i;

            if (variant.condition() != null) {
                String predFieldName = "pred" + i;
                if (variant.condition().negated()) {
                    builder.beginControlFlow("if (!$L.test(value))", predFieldName);
                } else {
                    builder.beginControlFlow("if ($L.test(value))", predFieldName);
                }
            } else {
                builder.beginControlFlow("if ($T.abs(value - $L) <= $L)",
                        Math.class, variant.doubleVariantValue(), variant.doubleDelta());
            }

            builder.addStatement("return $L.get()", supplierFieldName);
            builder.endControlFlow();
        }

        builder.endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.getAsDouble()))",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildMapBasedBooleanResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T<$T> flagValue = (context != null) ? flagProvider.getBoolean($S, context) : flagProvider.getBoolean($S)",
                        Optional.class, Boolean.class, model.flagKey(), model.flagKey());

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("$T supplier = variants.get(flagValue.get())", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.get()), variants.keySet())",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildOrderedBooleanResolveMethod(FeatureModel model, ClassName interfaceType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T<$T> flagValue = (context != null) ? flagProvider.getBoolean($S, context) : flagProvider.getBoolean($S)",
                        Optional.class, Boolean.class, model.flagKey(), model.flagKey());

        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        builder.beginControlFlow("if (flagValue.isPresent())")
                .addStatement("boolean value = flagValue.get()");

        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);
            String supplierFieldName = "variant" + i;

            if (variant.condition() != null) {
                String predFieldName = "pred" + i;
                if (variant.condition().negated()) {
                    builder.beginControlFlow("if (!$L.test(value))", predFieldName);
                } else {
                    builder.beginControlFlow("if ($L.test(value))", predFieldName);
                }
            } else {
                builder.beginControlFlow("if (value == $L)", variant.booleanVariantValue());
            }

            builder.addStatement("return $L.get()", supplierFieldName);
            builder.endControlFlow();
        }

        builder.endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder.beginControlFlow("if (flagValue.isEmpty())")
                    .addStatement("throw $T.noFlagValue($S)",
                            UnmatchedVariantException.class, model.flagKey())
                    .endControlFlow()
                    .addStatement("throw new $T($S, $T.valueOf(flagValue.get()))",
                            UnmatchedVariantException.class, model.flagKey(), String.class);
        }

        return builder.build();
    }

    private MethodSpec buildStringResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        if (model.hasOrderedDispatch()) {
            return buildOrderedStringResolveMethod(model, interfaceType);
        }
        return buildMapBasedStringResolveMethod(model, interfaceType, supplierType);
    }

    private MethodSpec buildMapBasedStringResolveMethod(FeatureModel model, ClassName interfaceType, ParameterizedTypeName supplierType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T<$T> flagValue = (context != null) ? flagProvider.getString($S, context) : flagProvider.getString($S)",
                        Optional.class, String.class, model.flagKey(), model.flagKey())
                .addStatement("$T rawValue = flagValue.orElse(null)", String.class);

        builder.beginControlFlow("if (rawValue != null)")
                .addStatement("$T supplier = variants.get(rawValue)", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder
                .beginControlFlow("if (rawValue == null)")
                .addStatement("throw $T.noFlagValue($S)",
                        UnmatchedVariantException.class, model.flagKey())
                .endControlFlow()
                .addStatement("throw new $T($S, rawValue, variants.keySet())",
                        UnmatchedVariantException.class, model.flagKey());
        }

        return builder.build();
    }

    private MethodSpec buildOrderedStringResolveMethod(FeatureModel model, ClassName interfaceType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T<$T> flagValue = (context != null) ? flagProvider.getString($S, context) : flagProvider.getString($S)",
                        Optional.class, String.class, model.flagKey(), model.flagKey())
                .addStatement("$T rawValue = flagValue.orElse(null)", String.class);

        // Sort variants by order for evaluation sequence
        List<VariantModel> sortedVariants = model.variants().stream()
                .sorted(Comparator.comparingInt(VariantModel::order))
                .toList();

        builder.beginControlFlow("if (rawValue != null)");

        // Generate ordered if-else chain
        for (int i = 0; i < sortedVariants.size(); i++) {
            VariantModel variant = sortedVariants.get(i);
            String supplierFieldName = "variant" + i;

            if (variant.condition() != null) {
                // Condition-based: predicate.test(rawValue) or !predicate.test(rawValue)
                String predFieldName = "pred" + i;
                if (variant.condition().negated()) {
                    builder.beginControlFlow("if (!$L.test(rawValue))", predFieldName);
                } else {
                    builder.beginControlFlow("if ($L.test(rawValue))", predFieldName);
                }
            } else {
                // Exact match: rawValue.equals("value")
                builder.beginControlFlow("if (rawValue.equals($S))", variant.variantValue());
            }

            builder.addStatement("return $L.get()", supplierFieldName);
            builder.endControlFlow();
        }

        builder.endControlFlow();

        builder.beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder
                .beginControlFlow("if (rawValue == null)")
                .addStatement("throw $T.noFlagValue($S)",
                        UnmatchedVariantException.class, model.flagKey())
                .endControlFlow()
                .addStatement("throw new $T($S, rawValue)",
                        UnmatchedVariantException.class, model.flagKey());
        }

        return builder.build();
    }

    private MethodSpec buildDispatchMethod(MethodModel method, ClassName interfaceType, FallbackStrategy fallbackStrategy) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(method.name())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        // Add parameters
        for (MethodModel.ParameterModel param : method.parameters()) {
            builder.addParameter(resolveTypeName(param.type()), param.name());
        }

        String paramNames = method.parameters().stream()
                .map(MethodModel.ParameterModel::name)
                .collect(Collectors.joining(", "));

        if (fallbackStrategy == FallbackStrategy.NOOP) {
            buildNoopDispatch(builder, method, interfaceType, paramNames.toString());
        } else if (method.isVoid()) {
            builder.addStatement("resolveVariant().$L($L)", method.name(), paramNames);
        } else {
            builder.returns(resolveTypeName(method.returnType()));
            builder.addStatement("return resolveVariant().$L($L)", method.name(), paramNames);
        }

        return builder.build();
    }

    private void buildNoopDispatch(MethodSpec.Builder builder, MethodModel method,
                                    ClassName interfaceType, String paramNames) {
        builder.addStatement("$T delegate = resolveVariant()", interfaceType);

        if (method.isVoid()) {
            builder.beginControlFlow("if (delegate != null)")
                    .addStatement("delegate.$L($L)", method.name(), paramNames)
                    .endControlFlow();
        } else {
            TypeName returnType = resolveTypeName(method.returnType());
            builder.returns(returnType);
            builder.beginControlFlow("if (delegate != null)")
                    .addStatement("return delegate.$L($L)", method.name(), paramNames)
                    .endControlFlow()
                    .addStatement("return $L", defaultValueFor(method.returnType()));
        }
    }

    private static TypeName resolveTypeName(String typeName) {
        return switch (typeName) {
            case "boolean" -> TypeName.BOOLEAN;
            case "byte" -> TypeName.BYTE;
            case "short" -> TypeName.SHORT;
            case "int" -> TypeName.INT;
            case "long" -> TypeName.LONG;
            case "float" -> TypeName.FLOAT;
            case "double" -> TypeName.DOUBLE;
            case "char" -> TypeName.CHAR;
            case "void" -> TypeName.VOID;
            default -> ClassName.bestGuess(typeName);
        };
    }

    private static String defaultValueFor(String typeName) {
        return switch (typeName) {
            case "boolean" -> "false";
            case "byte", "short", "int" -> "0";
            case "long" -> "0L";
            case "float" -> "0.0f";
            case "double" -> "0.0d";
            case "char" -> "'\\0'";
            default -> "null";
        };
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
            // Convert Map<String, Supplier<T>> to Map<Integer, Supplier<T>>
            ParameterizedTypeName intMapType = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Integer.class),
                    supplierType
            );
            builder.addStatement("$T intVariants = new $T<>()", intMapType, ClassName.get("java.util", "HashMap"));
            builder.addStatement("variants.forEach((k, v) -> intVariants.put($T.parseInt(k), v))", Integer.class);
            builder.addStatement("return new $T(flagProvider, intVariants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        } else if (model.featureType() == FeatureType.LONG) {
            // Convert Map<String, Supplier<T>> to Map<Long, Supplier<T>>
            ParameterizedTypeName longMapType = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Long.class),
                    supplierType
            );
            builder.addStatement("$T longVariants = new $T<>()", longMapType, ClassName.get("java.util", "HashMap"));
            builder.addStatement("variants.forEach((k, v) -> longVariants.put($T.parseLong(k), v))", Long.class);
            builder.addStatement("return new $T(flagProvider, longVariants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        } else if (model.featureType() == FeatureType.DOUBLE) {
            // Convert Map<String, Supplier<T>> to Map<Double, Supplier<T>>
            ParameterizedTypeName doubleMapType = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Double.class),
                    supplierType
            );
            builder.addStatement("$T doubleVariants = new $T<>()", doubleMapType, ClassName.get("java.util", "HashMap"));
            builder.addStatement("variants.forEach((k, v) -> doubleVariants.put($T.parseDouble(k), v))", Double.class);
            builder.addStatement("return new $T(flagProvider, doubleVariants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        } else if (model.featureType() == FeatureType.BOOLEAN) {
            // Convert Map<String, Supplier<T>> to Map<Boolean, Supplier<T>>
            ParameterizedTypeName boolMapType = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Boolean.class),
                    supplierType
            );
            builder.addStatement("$T boolVariants = new $T<>()", boolMapType, ClassName.get("java.util", "HashMap"));
            builder.addStatement("variants.forEach((k, v) -> boolVariants.put($T.parseBoolean(k), v))", Boolean.class);
            builder.addStatement("return new $T(flagProvider, boolVariants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        } else {
            builder.addStatement("return new $T(flagProvider, variants, defaultVariant, $T.$L)",
                    proxyType, FallbackStrategy.class, model.fallbackStrategy().name());
        }

        return builder.build();
    }

    private static TypeName variantKeyType(FeatureType featureType) {
        return switch (featureType) {
            case INT -> ClassName.get(Integer.class);
            case LONG -> ClassName.get(Long.class);
            case BOOLEAN -> ClassName.get(Boolean.class);
            case DOUBLE -> ClassName.get(Double.class);
            default -> ClassName.get(String.class);
        };
    }

    private MethodSpec buildDefaultVariantSupplierMethod(FeatureModel model, ClassName interfaceType,
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
