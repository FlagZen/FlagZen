package com.flagzen.processor;

import com.flagzen.EvaluationContext;
import com.flagzen.FallbackStrategy;
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
import java.util.Map;
import java.util.Optional;
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
        MethodSpec.Builder builder = MethodSpec.methodBuilder("resolveVariant")
                .addModifiers(Modifier.PRIVATE)
                .returns(interfaceType)
                .addStatement("$T context = $T.current()", EvaluationContext.class, FlagContext.class)
                .addStatement("$T<$T> flagValue = (context != null) ? flagProvider.getString($S, context) : flagProvider.getString($S)",
                        Optional.class, String.class, model.flagKey(), model.flagKey())
                .addStatement("$T value = flagValue.orElse(null)", String.class)
                .beginControlFlow("if (value != null)")
                .addStatement("$T supplier = variants.get(value)", supplierType)
                .beginControlFlow("if (supplier != null)")
                .addStatement("return supplier.get()")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("if (defaultVariant != null)")
                .addStatement("return defaultVariant.get()")
                .endControlFlow();

        if (model.fallbackStrategy() == FallbackStrategy.NOOP) {
            builder.addStatement("return null");
        } else {
            builder
                .beginControlFlow("if (value == null)")
                .addStatement("throw $T.noFlagValue($S)",
                        UnmatchedVariantException.class, model.flagKey())
                .endControlFlow()
                .addStatement("throw new $T($S, value, variants.keySet())",
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
