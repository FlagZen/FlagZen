package com.flagzen.acceptance.fixtures;

import java.util.function.Predicate;

/**
 * Predicate that matches enterprise tier values.
 * Simulates a user-defined predicate for condition-based dispatch.
 */
public class IsEnterpriseTier implements Predicate<String> {

    @Override
    public boolean test(String value) {
        return "enterprise".equalsIgnoreCase(value);
    }

    @Override
    public String toString() {
        return "IsEnterpriseTier";
    }
}
