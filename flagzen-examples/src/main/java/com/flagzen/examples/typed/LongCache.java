package com.flagzen.examples.typed;

import com.flagzen.Variant;

@Variant(intValue = {3600}, of = CacheStrategy.class)
public class LongCache implements CacheStrategy {
    @Override
    public int ttlSeconds() { return 3600; }
}
