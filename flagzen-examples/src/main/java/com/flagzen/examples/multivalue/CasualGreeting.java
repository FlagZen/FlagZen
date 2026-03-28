package com.flagzen.examples.multivalue;

import com.flagzen.Variant;

@Variant(value = {"CASUAL", "FRIENDLY"}, of = Greeting.class)
public class CasualGreeting implements Greeting {
    @Override
    public String greet(String name) {
        return "Hey " + name + "!";
    }
}
