package com.flagzen.examples.typed;

import com.flagzen.Variant;

@Variant(intValue = {60}, of = CacheStrategy.class)
public class ShortCache implements CacheStrategy {
    @Override
    public int ttlSeconds() { return 60; }
}
