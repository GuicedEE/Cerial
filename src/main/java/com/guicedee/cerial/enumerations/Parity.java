package com.guicedee.cerial.enumerations;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Supported parity modes for serial port configuration.
 */
public enum Parity
{
    /** No parity. */
    None(0),
    /** Odd parity. */
    Odd(1),
    /** Even parity. */
    Even(2),
    /** Mark parity. */
    Mark(3),
    /** Space parity. */
    Space(4);

    /** Numeric parity identifier used by underlying libraries. */
    private final int parity;

    /**
     * Creates a parity enum with its numeric identifier.
     *
     * @param parity the parity identifier
     */
    Parity(int parity)
    {
        this.parity = parity;
    }

    /**
     * Returns the numeric parity identifier.
     *
     * @return parity identifier as an integer
     */
    public int toInt()
    {
        return parity;
    }

    @JsonCreator
    /**
     * Parses parity from a numeric or textual value.
     *
     * @param value numeric value (e.g., "2") or name (e.g., "Even")
     * @return the matching {@link Parity}, or null if no match
     */
    public static Parity from(String value)
    {
        if (NumberUtils.isCreatable(value))
        {
            for (Parity parity : Parity.values())
            {
                if (parity.parity == Integer.parseInt(value))
                {
                    return parity;
                }
            }
        }
        else {
            for (Parity parity : Parity.values())
            {
                if (parity.name()
                          .equalsIgnoreCase(value))
                {
                    return parity;
                }
            }

        }
        return null;
    }
}
