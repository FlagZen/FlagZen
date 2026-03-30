package com.flagzen.examples.conditions;

import com.flagzen.Feature;
import com.flagzen.FeatureType;

@Feature(value = "max-retries", type = FeatureType.INT)
public interface RetryStrategy {
    int maxRetries();
    String description();
}
