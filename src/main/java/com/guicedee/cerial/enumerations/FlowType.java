package com.guicedee.cerial.enumerations;

/**
 * High-level flow control types used for connection configuration.
 */
public enum FlowType
{
     /** No flow control. */
     None,
     /** RTS/CTS hardware flow control. */
     RTSCTS,
     /** XON/XOFF software flow control. */
     XONXOFF,;

}
