package com.guicedee.cerial.enumerations;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Supported data bit lengths for serial port configuration.
 */
public enum DataBits
{
    /** 5 data bits. */
    $5(5),
    /** 6 data bits. */
    $6(6),
    /** 7 data bits. */
    $7(7),
    /** 8 data bits. */
    $8(8);

    /** Numeric data bit count. */
    private final int bits;

    /**
     * Creates a data-bit enum with its numeric representation.
     *
     * @param bits the data bit count
     */
    DataBits(int bits)
    {
        this.bits = bits;
    }

    /**
     * Returns the numeric data bit count.
     *
     * @return the data bit count as an integer
     */
    public int toInt()
    {
        return bits;
    }

    /**
     * Returns the numeric data bit count as a string without the enum prefix.
     *
     * @return data bit count as a string (e.g., "8")
     */
    @Override
    public String toString()
    {
        return name().replace("$", "");
    }

    @JsonCreator
    /**
     * Parses a data-bit value from a string, with or without the leading '$'.
     *
     * @param s the string value (e.g., "8" or "$8")
     * @return the matching {@link DataBits}, or null if input is null
     */
    public static DataBits fromString(String s)
    {
        if(s == null)
        {
            return null;
        }
        return DataBits.valueOf((s.startsWith("$") ? "" : "$") + s.toUpperCase());
    }

}
