package com.guicedee.cerial.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import lombok.extern.java.Log;

/**
 * Guice module that binds a range of serial port connections by port number.
 * Each binding uses a provider that constructs the connection on demand and scopes
 * it as a singleton for that named port.
 */
@Log
public class CerialPortsBindings extends AbstractModule implements IGuiceModule<CerialPortsBindings>
{
    /**
     * Registers bindings for a wide range of serial port numbers.
     */
    @Override
    protected void configure()
    {
        for (int i = 0; i < 1000; i++)
        {
            bind(Key.get(CerialPortConnection.class, Names.named(i + ""))).toProvider(new CerialPortConnectionProvider(i))
                                                                          .in(Singleton.class);
        }
        log.fine("Bound the collection of possible serial port numbers to a dynamic singleton provider");
    }


}
