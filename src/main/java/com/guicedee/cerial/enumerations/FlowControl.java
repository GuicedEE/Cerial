package com.guicedee.cerial.enumerations;

/**
 * Supported flow control bitmask values for serial port configuration.
 */
public enum FlowControl
{
     /** No flow control. */
     None(0),
     /** RTS/CTS input flow control. */
     RTSCTS_IN(1),
     /** RTS/CTS output flow control. */
     RTSCTS_OUT(2),
     /** XON/XOFF input flow control. */
     XONXOFF_IN(4),
     /** XON/XOFF output flow control. */
     XONXOFF_OUT(8);

    /** Numeric flow control bitmask. */
    private final int flowControl;

    /**
     * Creates a flow control enum with its numeric bitmask.
     *
     * @param flowControl the flow control bitmask value
     */
    FlowControl(int flowControl)
    {
        this.flowControl = flowControl;
    }

    /**
     * Returns the numeric flow control bitmask.
     *
     * @return the flow control bitmask
     */
    public int toInt()
    {
        return flowControl;
    }
}
