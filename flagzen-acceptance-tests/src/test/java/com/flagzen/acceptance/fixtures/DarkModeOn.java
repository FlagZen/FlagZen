package com.flagzen.acceptance.fixtures;

/**
 * Test fixture: boolean variant activated when dark-mode flag = true.
 */
public class DarkModeOn implements DarkMode {
    @Override
    public void apply() {
        // variant implementation
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
