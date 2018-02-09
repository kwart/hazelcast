package com.hazelcast.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Sets or clears a property before running a test. The property will be restored once the test is finished.
 * <p>
 * Can be used for finer control of the scope of a System property.
 */
public final class OverridePropertyRule implements TestRule {

    private final String propertyName;
    private final String value;

    private OverridePropertyRule(String propertyName, String value) {
        this.propertyName = propertyName;
        this.value = value;
    }

    /**
     * Clears the property.
     *
     * @param propertyName system property to clear
     * @return instance of the rule
     */
    public static OverridePropertyRule clear(String propertyName) {
        return new OverridePropertyRule(propertyName, null);
    }

    /**
     * Set the property to a {@code newValue}.
     *
     * @param propertyName system property to set
     * @param newValue     value to set
     * @return instance of the rule
     */
    public static OverridePropertyRule set(String propertyName, String newValue) {
        return new OverridePropertyRule(propertyName, newValue);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                String oldValue = System.getProperty(propertyName);
                setOrClearProperty(propertyName, value);
                try {
                    base.evaluate();
                } finally {
                    setOrClearProperty(propertyName, oldValue);
                }
            }
        };
    }

    public String setOrClear(String newValue) {
        return setOrClearProperty(propertyName, newValue);
    }

    private static String setOrClearProperty(String propertyName, String value) {
        return value == null ? System.clearProperty(propertyName) : System.setProperty(propertyName, value);
    }
}
