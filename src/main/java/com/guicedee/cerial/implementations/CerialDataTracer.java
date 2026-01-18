package com.guicedee.cerial.implementations;

import com.fazecast.jSerialComm.SerialPort;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import io.opentelemetry.api.trace.Span;
import lombok.extern.log4j.Log4j2;

import java.util.function.BiConsumer;

/**
 * Concrete implementation for tracing serial data reception.
 */
@Log4j2
public class CerialDataTracer {

    /**
     * Traces the received data and delegates to the provided comPortRead consumer.
     *
     * @param newData      The data received.
     * @param connection   The connection it was received on.
     * @param comPortRead  The callback to invoke.
     */
    @Trace("Serial Receive")
    public void onDataReceived(@SpanAttribute("serial.data") byte[] newData, CerialPortConnection<?> connection, BiConsumer<byte[], SerialPort> comPortRead) {
        Span.current().setAttribute("serial.port", connection.getComPortName());
        Span.current().setAttribute("serial.message_length", newData.length);
        
        if (comPortRead != null) {
            try {
                comPortRead.accept(newData, connection.getConnectionPort());
            } catch (Throwable e) {
                log.fatal("Fatal error in ComPortRead handler on ComPort [{}]: {}", connection.getComPort(), e.getMessage(), e);
            }
        }
    }
}
