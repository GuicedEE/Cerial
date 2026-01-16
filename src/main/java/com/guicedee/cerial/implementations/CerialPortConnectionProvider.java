package com.guicedee.cerial.implementations;

import com.google.inject.Provider;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.cerial.enumerations.BaudRate;

/**
 * Guice provider that creates a {@link CerialPortConnection} for a specific COM port.
 */
public class CerialPortConnectionProvider implements Provider<CerialPortConnection>
{
    private final int comPortNumber;

    /**
     * Creates a provider for a specific port number.
     *
     * @param comPortNumber the COM port number to bind
     */
    public CerialPortConnectionProvider(int comPortNumber)
    {
        this.comPortNumber = comPortNumber;
    }
    @Override

    /**
     * Builds a new connection using a default baud rate.
     *
     * @return a new {@link CerialPortConnection} instance
     */
    public CerialPortConnection get()
    {
        CerialPortConnection cerialPortConnection = new CerialPortConnection(comPortNumber, BaudRate.$9600);
        return cerialPortConnection;
    }

}
