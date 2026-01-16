package com.guicedee.cerial.implementations;

import com.fazecast.jSerialComm.SerialPortDataListener;

/**
 * Common contract for serial port listeners that expose a read callback.
 */
public interface ComPortEvents
{
    /**
     * Returns the callback invoked when a message or byte sequence is received.
     *
     * @return the current read callback, or null if not configured
     */
    java.util.function.BiConsumer<byte[], com.fazecast.jSerialComm.SerialPort> getComPortRead();

    /**
     * Sets the callback invoked when a message or byte sequence is received.
     *
     * @param comPortRead the callback to invoke with data and port
     * @return the listener instance for fluent use
     */
    SerialPortDataListener setComPortRead(java.util.function.BiConsumer<byte[], com.fazecast.jSerialComm.SerialPort> comPortRead);
}
