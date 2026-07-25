package com.guicedee.cerial.implementations;

import com.guicedee.cerial.CerialConnectionRegistry;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import lombok.extern.log4j.Log4j2;

import java.util.Set;

/**
 * Closes all active {@link CerialPortConnection} instances on application shutdown.
 * <p>
 * A single service iterates the {@link CerialConnectionRegistry} rather than each
 * connection registering itself as a pre-destroy service, which previously caused the
 * pre-destroy set to grow without bound.
 */
@Log4j2
public class CerialPreDestroy implements IGuicePreDestroy<CerialPreDestroy>
{
    @Override
    public void onDestroy()
    {
        // Snapshot to avoid concurrent modification while onDestroy() unregisters each connection.
        Set<CerialPortConnection<?>> connections = Set.copyOf(CerialConnectionRegistry.getActiveConnections());
        log.info("🛑 Shutting down {} serial port connection(s)...", connections.size());

        for (CerialPortConnection<?> connection : connections)
        {
            try
            {
                connection.onDestroy();
            }
            catch (Throwable t)
            {
                log.error("❌ Failed to close serial port connection: {}", t.getMessage(), t);
            }
        }

        log.info("✅ Serial port connection shutdown complete.");
    }

    @Override
    public Integer sortOrder()
    {
        return Integer.MAX_VALUE - 100;
    }
}

