package com.flagzen.processor;

import java.util.List;

/**
 * Compile-time model of a method declared on a @Feature interface.
 */
record MethodModel(String name, String returnType, List<ParameterModel> parameters) {

    boolean isVoid() {
        return "void".equals(returnType);
    }

    record ParameterModel(String type, String name) {}
}
