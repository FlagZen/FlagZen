package com.flagzen.processor;

import java.util.List;

/**
 * Compile-time model of a method declared on a @Feature interface.
 */
final class MethodModel {
    private final String name;
    private final String returnType;
    private final List<ParameterModel> parameters;

    MethodModel(String name, String returnType, List<ParameterModel> parameters) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    String name() { return name; }
    String returnType() { return returnType; }
    List<ParameterModel> parameters() { return parameters; }
    boolean isVoid() { return "void".equals(returnType); }

    record ParameterModel(String type, String name) {}
}
