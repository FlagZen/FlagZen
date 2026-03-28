package com.flagzen.examples.typed;

import com.flagzen.Feature;
import com.flagzen.FeatureType;

@Feature(value = "cache-ttl", type = FeatureType.INT)
public interface CacheStrategy {
    int ttlSeconds();
}
