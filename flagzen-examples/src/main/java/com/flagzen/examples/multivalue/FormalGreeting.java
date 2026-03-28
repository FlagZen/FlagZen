package com.flagzen.examples.multivalue;

import com.flagzen.Variant;

@Variant(value = {"FORMAL", "BUSINESS", "PROFESSIONAL"}, of = Greeting.class)
public class FormalGreeting implements Greeting {
    @Override
    public String greet(String name) {
        return "Dear " + name;
    }
}
