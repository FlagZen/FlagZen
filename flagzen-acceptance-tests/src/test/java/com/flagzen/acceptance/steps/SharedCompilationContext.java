package com.flagzen.acceptance.steps;

import com.google.testing.compile.Compilation;

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
     * Executes all registered pre-compile hooks.
     * Called by {@link CompileTimeSteps#theProjectCompiles()} before compilation.
     */
    static void runPreCompileHooks() {
        for (Runnable hook : PRE_COMPILE_HOOKS.get()) {
            hook.run();
        }
        PRE_COMPILE_HOOKS.get().clear();
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
    }
}
