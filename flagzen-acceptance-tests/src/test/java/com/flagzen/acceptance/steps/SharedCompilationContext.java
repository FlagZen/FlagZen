package com.flagzen.acceptance.steps;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thread-safe shared context for source files and compilation results across step definition classes.
 * Reset before each scenario via {@link ScenarioHooks}.
 */
final class SharedCompilationContext {

    private static final ThreadLocal<List<JavaFileObject>> SOURCE_FILES =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Set<String>> ADDED_SOURCES =
            ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Compilation> COMPILATION = new ThreadLocal<>();
    private static final ThreadLocal<List<Runnable>> PRE_COMPILE_HOOKS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Map<String, String>> FEATURE_TYPES =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, String>> FEATURE_METHODS =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, String>> VARIANT_ENUM_BLOCKS =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, List<String[]>>> REPEATED_VARIANT_ANNOTATIONS =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, String>> FEATURE_KEYS =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, String>> FEATURE_FALLBACKS =
            ThreadLocal.withInitial(HashMap::new);

    private SharedCompilationContext() {
    }

    static void addSourceFile(JavaFileObject source) {
        SOURCE_FILES.get().add(source);
    }

    static boolean isSourceAdded(String qualifiedName) {
        return ADDED_SOURCES.get().contains(qualifiedName);
    }

    static void markSourceAdded(String qualifiedName) {
        ADDED_SOURCES.get().add(qualifiedName);
    }

    static List<JavaFileObject> getSourceFiles() {
        return SOURCE_FILES.get();
    }

    /**
     * Registers a callback to be executed before compilation.
     * Used for deferred source generation (e.g., multi-feature variants).
     */
    static void addPreCompileHook(Runnable hook) {
        PRE_COMPILE_HOOKS.get().add(hook);
    }

    /**
     * Executes all registered pre-compile hooks and processes repeated variant annotations.
     * Called by {@link CompileTimeSteps#theProjectCompiles()} before compilation.
     */
    static void runPreCompileHooks() {
        for (Runnable hook : PRE_COMPILE_HOOKS.get()) {
            hook.run();
        }
        PRE_COMPILE_HOOKS.get().clear();
        processRepeatedVariantAnnotations();
    }

    private static void processRepeatedVariantAnnotations() {
        Map<String, List<String[]>> repeatedAnnotations = REPEATED_VARIANT_ANNOTATIONS.get();
        if (repeatedAnnotations.isEmpty()) {
            return;
        }
        // For each variant class with repeated annotations, find its existing source
        // and rebuild it with the additional @Variant annotations
        List<JavaFileObject> existingSources = SOURCE_FILES.get();
        for (Map.Entry<String, List<String[]>> entry : repeatedAnnotations.entrySet()) {
            String qualifiedName = entry.getKey();
            List<String[]> annotations = entry.getValue();

            // Find the existing source by checking each source's name
            JavaFileObject originalSource = null;
            int originalIndex = -1;
            for (int i = 0; i < existingSources.size(); i++) {
                String uri = existingSources.get(i).toUri().toString();
                // JavaFileObjects.forSourceString creates URIs like /<qualified.name>
                if (uri.contains(qualifiedName) || uri.contains(qualifiedName.replace('.', '/'))) {
                    originalSource = existingSources.get(i);
                    originalIndex = i;
                    break;
                }
            }
            if (originalSource == null) {
                continue;
            }

            try {
                String source = originalSource.getCharContent(false).toString();
                // Insert additional @Variant annotations before the class declaration
                StringBuilder extraAnnotations = new StringBuilder();
                for (String[] ann : annotations) {
                    String interfaceName = ann[0];
                    String valueArray = ann[1];
                    extraAnnotations.append("@Variant(value = %s, of = %s.class)\n".formatted(
                            valueArray, interfaceName));
                }
                // Insert before "public class"
                String modified = source.replace("public class",
                        extraAnnotations + "public class");
                existingSources.set(originalIndex, JavaFileObjects.forSourceString(qualifiedName, modified));
            } catch (Exception e) {
                throw new RuntimeException("Failed to process repeated variant annotations", e);
            }
        }
        repeatedAnnotations.clear();
    }

    /**
     * Records the FeatureType for a feature interface name.
     * Used by TypeAnnotationSteps to communicate type info to CompileTimeSteps.
     */
    static void setFeatureType(String featureName, String featureType) {
        FEATURE_TYPES.get().put(featureName, featureType);
    }

    /**
     * Returns the FeatureType for a feature interface name, or null if not set.
     */
    static String getFeatureType(String featureName) {
        return FEATURE_TYPES.get().get(featureName);
    }

    static void setFeatureMethod(String featureName, String methodName) {
        FEATURE_METHODS.get().put(featureName, methodName);
    }

    static String getFeatureMethod(String featureName) {
        return FEATURE_METHODS.get().get(featureName);
    }

    static void setFeatureKey(String featureName, String key) {
        FEATURE_KEYS.get().put(featureName, key);
    }

    static String getFeatureKey(String featureName) {
        return FEATURE_KEYS.get().get(featureName);
    }

    static void setFeatureFallback(String featureName, String fallback) {
        FEATURE_FALLBACKS.get().put(featureName, fallback);
    }

    static String getFeatureFallback(String featureName) {
        return FEATURE_FALLBACKS.get().get(featureName);
    }

    /**
     * Ensures a feature interface source file is generated if not already added.
     * Called from variant step definitions that need the feature source to compile.
     */
    static void ensureFeatureSource(String featureName) {
        String qualifiedName = "com.example." + featureName;
        if (isSourceAdded(qualifiedName)) {
            return;
        }
        markSourceAdded(qualifiedName);

        String key = getFeatureKey(featureName);
        if (key == null) {
            return;
        }
        String featureType = getFeatureType(featureName);
        String fallback = getFeatureFallback(featureName);
        String enumBlock = getVariantEnumBlock(featureName);
        if (enumBlock == null) {
            enumBlock = "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package com.example;\n\nimport com.flagzen.Feature;\n");
        if (featureType != null) {
            sb.append("import com.flagzen.FeatureType;\n");
        }
        if (fallback != null) {
            sb.append("import com.flagzen.FallbackStrategy;\n");
        }
        sb.append("\n@Feature(value = \"").append(key).append("\"");
        if (featureType != null) {
            sb.append(", type = FeatureType.").append(featureType);
        }
        if (fallback != null) {
            sb.append(", fallback = FallbackStrategy.").append(fallback);
        }
        sb.append(")\npublic interface ").append(featureName).append(" {\n");
        sb.append(enumBlock);
        sb.append("}\n");

        addSourceFile(JavaFileObjects.forSourceString(qualifiedName, sb.toString()));
    }

    static void setVariantEnumBlock(String featureName, String enumBlock) {
        VARIANT_ENUM_BLOCKS.get().put(featureName, enumBlock);
    }

    static String getVariantEnumBlock(String featureName) {
        return VARIANT_ENUM_BLOCKS.get().get(featureName);
    }

    /**
     * Records a repeated @Variant annotation to be added to a variant class at compile time.
     * The source file for the variant will be rebuilt with all annotations during pre-compile hooks.
     */
    static void addRepeatedVariantAnnotation(String qualifiedName, String interfaceName, String valueArray) {
        REPEATED_VARIANT_ANNOTATIONS.get()
                .computeIfAbsent(qualifiedName, k -> new ArrayList<>())
                .add(new String[]{interfaceName, valueArray});
    }

    static Compilation getCompilation() {
        return COMPILATION.get();
    }

    static void setCompilation(Compilation compilation) {
        COMPILATION.set(compilation);
    }

    static void reset() {
        SOURCE_FILES.get().clear();
        ADDED_SOURCES.get().clear();
        COMPILATION.remove();
        PRE_COMPILE_HOOKS.get().clear();
        FEATURE_TYPES.get().clear();
        FEATURE_METHODS.get().clear();
        VARIANT_ENUM_BLOCKS.get().clear();
        REPEATED_VARIANT_ANNOTATIONS.get().clear();
        FEATURE_KEYS.get().clear();
        FEATURE_FALLBACKS.get().clear();
    }
}
