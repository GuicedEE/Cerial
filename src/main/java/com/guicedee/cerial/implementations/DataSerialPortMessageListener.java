package com.guicedee.cerial.implementations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.google.common.base.Strings;
import com.guicedee.cerial.CerialPortConnection;
import com.guicedee.cerial.SerialPortException;
import com.guicedee.cerial.enumerations.ComPortStatus;
import com.guicedee.client.annotations.INotInjectable;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.utils.LogUtils;
import com.guicedee.client.scopes.CallScopeProperties;
import com.guicedee.client.scopes.CallScopeSource;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.core.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import static com.fazecast.jSerialComm.SerialPort.*;
import static com.guicedee.cerial.CerialPortConnection.portNumberFormat;
import static com.guicedee.cerial.enumerations.ComPortStatus.Running;

/**
 * Serial port listener that treats incoming data as message frames separated by a delimiter.
 * Uses jSerialComm message callbacks and dispatches the payload on a Vertx worker thread with
 * Guice call-scope metadata populated for downstream handlers.
 */
@Getter
@Setter
@INotInjectable
public class DataSerialPortMessageListener implements SerialPortMessageListener, ComPortEvents
{
    @JsonIgnore
    private Logger log;

    @JsonIgnore
    
    private BiConsumer<byte[], SerialPort> comPortRead;
    @JsonIgnore
    
    private SerialPort comPort;
    @JsonIgnore
    
    private CerialPortConnection<?> connection;
    @JsonIgnore
    
    private char[] delimiter;

    /**
     * Creates a listener bound to a specific serial port and connection.
     *
     * @param delimiter  the message delimiter characters used by jSerialComm
     * @param comPort    the serial port instance being monitored
     * @param connection the owning connection for status and error reporting
     */
    public DataSerialPortMessageListener(char[] delimiter, SerialPort comPort, CerialPortConnection<?> connection)
    {
        this.delimiter = delimiter;
        this.comPort = comPort;
        this.connection = connection;
        String loggerName = (connection.getComPort() == 0) ? "cerial" : "COM" + connection.getComPort();
        log = LogUtils.getSpecificRollingLogger(loggerName, "cerial",
                "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%-5level] - [%msg]%n",false);
    }

    /**
     * Returns the delimiter bytes used by jSerialComm to split messages.
     *
     * @return the delimiter bytes, encoded using UTF-8
     */
    @Override
    public byte[] getMessageDelimiter()
    {
        return new String(delimiter).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Indicates the delimiter terminates a message.
     *
     * @return true to treat the delimiter as end-of-message
     */
    @Override
    public boolean delimiterIndicatesEndOfMessage()
    {
        return true;
    }

    /**
     * Specifies the jSerialComm events this listener wants to receive.
     *
     * @return bitmask of listening events
     */
    @Override
    public int getListeningEvents()
    {
        return LISTENING_EVENT_DATA_RECEIVED | LISTENING_EVENT_PORT_DISCONNECTED | LISTENING_EVENT_BREAK_INTERRUPT | LISTENING_EVENT_FRAMING_ERROR | LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR | LISTENING_EVENT_PARITY_ERROR | LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR;
    }

    /**
     * Removes all occurrences of a byte value from the array.
     *
     * @param array    source byte array
     * @param toRemove byte value to remove
     * @return a new byte array without the removed values
     */
    public static byte[] remove(byte[] array, byte toRemove)
    {
        List<Byte> byteList = new ArrayList<>(Arrays.asList(ArrayUtils.toObject(array)));
        byteList.removeIf(b -> b == toRemove);
        return ArrayUtils.toPrimitive(byteList.toArray(new Byte[0]));
    }

    /**
     * Handles serial port events and routes data or errors to the connection.
     *
     * @param event the jSerialComm event
     */
    @Override
    public void serialEvent(SerialPortEvent event)
    {
        if (event.getEventType() == LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR) {
            log.error("❌ Software Overrun Error: {}", event.toString());
            connection.onConnectError(new SerialPortException("Software Overrun Error - " + event.toString()), ComPortStatus.GeneralException);
        }
        else  if (event.getEventType() == LISTENING_EVENT_PARITY_ERROR) {
            log.error("❌ Software Parity Error: {}", event.toString());
            connection.onConnectError(new SerialPortException("Software Parity Error - " + event.toString()), ComPortStatus.GeneralException);
        }
        else  if (event.getEventType() == LISTENING_EVENT_FRAMING_ERROR) {
            log.error("❌ Hardware Framing Error: {}", event.toString());
            connection.onConnectError(new SerialPortException("Hardware Framing Error - " + event.toString()), ComPortStatus.GeneralException);
        }
        else  if (event.getEventType() == LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR) {
            log.error("❌ Hardware Firmware Overrun Error: {}", event.toString());
            connection.onConnectError(new SerialPortException("Hardware Firmware Overrun Error - " + event.toString()), ComPortStatus.GeneralException);
        }
        else  if (event.getEventType() == LISTENING_EVENT_BREAK_INTERRUPT) {
            log.error("Hardware Break Interrupt Error - " + event.toString());
            connection.onConnectError(new SerialPortException("Hardware Break Interrupt Error - " + event.toString()), ComPortStatus.GeneralException);
        } else if (event.getEventType() == LISTENING_EVENT_PORT_DISCONNECTED) {
            log.error("🔌 Port disconnected: {}", event.toString());
            connection.onConnectError(new SerialPortException("Port disconnected - " + event.toString()), ComPortStatus.Offline);
        } else if (event.getEventType() == LISTENING_EVENT_DATA_RECEIVED)
        {
            byte[] newData = event.getReceivedData();
            processReceivedBytes(newData);
        }
    }

    /**
     * Processes a received message payload and dispatches it via the configured consumer.
     *
     * @param newData the received message bytes
     */
    public void processReceivedBytes(byte[] newData)
    {
        remove(newData, (byte) 0);

        if (Strings.isNullOrEmpty(new String(newData).trim()))
            return;

        String rxMessage = new String(newData, StandardCharsets.UTF_8).trim();
        if (!rxMessage.isEmpty()) {
            log.info("📥 RX - Port {} - Message: {}", portNumberFormat.format(connection.getComPort()), rxMessage);
        }
        var vertx = IGuiceContext.get(Vertx.class);
        vertx.executeBlocking(() -> {
            com.guicedee.client.scopes.CallScoper callScoper = null;
            boolean started = false;
            try {
                callScoper = IGuiceContext.get(com.guicedee.client.scopes.CallScoper.class);
                if (!callScoper.isStartedScope()) {
                    callScoper.enter();
                    started = true;
                }
                CallScopeProperties properties = IGuiceContext.get(CallScopeProperties.class);
                if (properties.getSource() == null || properties.getSource() == CallScopeSource.Unknown) {
                    properties.setSource(CallScopeSource.SerialPort);
                }
                properties.getProperties().put("ComPort", comPort);
                properties.getProperties().put("CerialPortConnection", connection);
                connection.setComPortStatus(Running);

                CerialPortConnection.addBytesRead(newData.length, connection.getComPortName());

                if (IGuiceContext.instance().getScanResult().getClassesImplementing(com.guicedee.client.services.lifecycle.IGuiceModule.class).loadClasses().stream().anyMatch(c -> c.getSimpleName().equals("TraceModule")))
                {
                    IGuiceContext.get(CerialDataTracer.class).onDataReceived(newData, connection, getComPortRead());
                }else {
                    if(getComPortRead() != null)
                        getComPortRead().accept(newData, getComPort());
                }

            } catch (Throwable T) {
                log.error("Error on ComPort [" + connection.getComPort() + "] Receipt", T);
            } finally {
                if (started && callScoper != null) {
                    callScoper.exit();
                }
            }
            return null;
        }, false);
    }
}
