package com.flagzen.examples.conditions;

import java.util.function.IntPredicate;

/**
 * Matches retry counts of 7 or more. Used as a condition predicate
 * for aggressive retry strategies.
 */
public class HighRetryRange implements IntPredicate {
    @Override
    public boolean test(int value) {
        return value >= 7;
    }
}
