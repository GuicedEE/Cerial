package com.guicedee.cerial.implementations;

import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;

/**
 * Concrete class to handle tracing of data written to serial ports.
 * This allows GuicedEE's telemetry interceptors to manage spans and attributes.
 */
@Log4j2
public class CerialWriteTracer {

    public static java.lang.invoke.MethodHandles.Lookup getModuleLookup() {
        return java.lang.invoke.MethodHandles.lookup();
    }

    /**
     * Traces the write operation and records message details.
     *
     * @param message The message being written
     * @param portName The name of the COM port
     * @param portNumber The port number for logging
     */
    @Trace("Serial Write")
    public void onWrite(@SpanAttribute("message") String message, @SpanAttribute("serial.port") String portName, @SpanAttribute("serial.portNumber") Integer portNumber) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        Span.current().setAttribute("serial.message_length", bytes.length);
        // Logging is handled in CerialPortConnection for now to maintain consistency with existing patterns,
        // but attributes are captured here via AOP and manual Span.current() calls.
    }
}
