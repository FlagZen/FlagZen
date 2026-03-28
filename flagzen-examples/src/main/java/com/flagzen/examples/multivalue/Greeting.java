package com.flagzen.examples.multivalue;

import com.flagzen.Feature;

@Feature("greeting-style")
public interface Greeting {
    String greet(String name);
}
