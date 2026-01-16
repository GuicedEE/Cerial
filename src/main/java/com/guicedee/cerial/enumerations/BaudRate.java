package com.guicedee.cerial.enumerations;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Supported baud rates for serial port configuration.
 */
public enum BaudRate
{
    /** 300 bps. */
    $300,
    /** 600 bps. */
    $600,
    /** 1200 bps. */
    $1200,
    /** 4800 bps. */
    $4800,
    /** 9600 bps. */
    $9600,
    /** 14400 bps. */
    $14400,
    /** 19200 bps. */
    $19200,
    /** 38400 bps. */
    $38400,
    /** 57600 bps. */
    $57600,
    /** 115200 bps. */
    $115200,
    /** 128000 bps. */
    $128000,
    /** 256000 bps. */
    $256000;

    /**
     * Returns the numeric baud rate as a string without the enum prefix.
     *
     * @return baud rate as a string (e.g., "9600")
     */
    @Override
    public String toString()
    {
        return name().replace("$", "");
    }

    /**
     * Returns the numeric baud rate.
     *
     * @return baud rate as an integer
     */
    public int toInt()
    {
        return Integer.parseInt(toString());
    }

    @JsonCreator
    /**
     * Parses a baud rate from a string, with or without the leading '$'.
     *
     * @param s the string value (e.g., "9600" or "$9600")
     * @return the matching {@link BaudRate}, or null if input is null
     */
    public static BaudRate from(String s)
    {
        if (s == null)
        {
            return null;
        }
        return BaudRate.valueOf((s.startsWith("$") ? "" : "$") + s.toUpperCase());
    }

}
