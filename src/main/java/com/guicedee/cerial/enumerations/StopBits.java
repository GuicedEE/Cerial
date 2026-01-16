package com.guicedee.cerial.enumerations;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Supported stop bit configurations for serial port setup.
 */
public enum StopBits
{
     /** One stop bit. */
     $1(1),
     /** Two stop bits. */
     $2(2),
     /** One and a half stop bits. */
     $1_5(3);

    /** Numeric stop bit identifier used by underlying libraries. */
    private final int stopBitsValue;

    /**
     * Creates a stop-bits enum with its numeric identifier.
     *
     * @param stopBitsValue the stop bits identifier
     */
    StopBits(int stopBitsValue)
    {
        this.stopBitsValue = stopBitsValue;
    }

    /**
     * Returns the numeric stop bits identifier.
     *
     * @return stop bits identifier as an integer
     */
    public int toInt()
    {
        return stopBitsValue;
    }

    @JsonCreator
    /**
     * Parses a stop-bits value from a string, with or without the leading '$'.
     *
     * @param name the string value (e.g., "1", "2", or "1_5")
     * @return the matching {@link StopBits}, or null if input is null
     */
    public static StopBits from(String name)
    {
        if (name == null)
        {
            return null;
        }
        return valueOf((name.startsWith("$") ? "" : "$") + name.toUpperCase());
    }
}
