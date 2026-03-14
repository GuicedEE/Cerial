package com.guicedee.cerial;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.google.common.base.Strings;
import com.guicedee.cerial.enumerations.*;
import com.guicedee.cerial.implementations.ComPortEvents;
import com.guicedee.cerial.implementations.DataSerialPortMessageListener;
import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePreDestroy;
import com.guicedee.client.utils.LogUtils;
import com.guicedee.modules.services.jsonrepresentation.IJsonRepresentation;
import io.vertx.core.Vertx;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.function.TriConsumer;

import java.io.OutputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.guicedee.cerial.enumerations.ComPortStatus.*;

/**
 * Main class for managing serial port connections in the GuicedCerial module.
 * <p>
 * This class provides a high-level API for configuring, connecting to, and communicating
 * with serial ports. It integrates with the GuicedInjection framework for dependency
 * injection and lifecycle management.
 * <p>
 * Features include:
 * <ul>
 *   <li>Configuration of serial port parameters (baud rate, data bits, parity, stop bits, flow control)</li>
 *   <li>Connection management (connect, disconnect)</li>
 *   <li>Data transmission and reception</li>
 *   <li>Status monitoring and event handling</li>
 *   <li>Automatic lifecycle management</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * CerialPortConnection connection = new CerialPortConnection(1, BaudRate.$9600);
 * connection.setDataBits(DataBits.$8)
 *           .setParity(Parity.None)
 *           .setStopBits(StopBits.$1)
 *           .setFlowControl(FlowControl.None)
 *           .connect();
 *
 * // Send data
 * connection.write("Hello, world!");
 *
 * // Receive data
 * connection.setComPortRead((data, port) -> {
 *     String message = new String(data).trim();
 *     System.out.println("Received: " + message);
 * });
 * </pre>
 *
 * @param <J> The type of the implementing class for method chaining
 */
@SuppressWarnings({"UnusedReturnValue",
    "unchecked",
    "unused"})
@Getter
@Setter
@ToString(of = {"comPort",
    "comPortStatus"})
@JsonIgnoreProperties(ignoreUnknown = true, value = {"inspection"})
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@NoArgsConstructor
public class CerialPortConnection<J extends CerialPortConnection<J>> implements IJsonRepresentation<J>,
                                                                                    IGuicePreDestroy<J>
{
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Format for port numbers, ensuring they are displayed with 3 digits.
   */
  public static NumberFormat portNumberFormat = NumberFormat.getNumberInstance();

  /**
   * Logger for this connection.
   */
  @JsonIgnore
  private org.apache.logging.log4j.core.Logger log;

  static
  {
    portNumberFormat.setMinimumIntegerDigits(3);
    portNumberFormat.setMinimumFractionDigits(0);
    portNumberFormat.setMaximumFractionDigits(0);
    portNumberFormat.setMaximumIntegerDigits(3);
  }

  private static Object bytesWrittenCounter;
  private static Object bytesReadCounter;

  private static void initializeMetrics()
  {
    try
    {
      if (IGuiceContext.instance().getScanResult().getClassesImplementing(com.guicedee.client.services.lifecycle.IGuiceModule.class).loadClasses().stream().anyMatch(c -> c.getSimpleName().equals("TraceModule")))
      {
        bytesWrittenCounter = OpenTelemetrySDKConfigurator.getOpenTelemetry()
                                                          .getMeter("com.guicedee.cerial")
                                                          .counterBuilder("serial.bytes_written")
                                                          .setDescription("Total bytes written to serial ports")
                                                          .setUnit("bytes")
                                                          .build();

        bytesReadCounter = OpenTelemetrySDKConfigurator.getOpenTelemetry()
                                                       .getMeter("com.guicedee.cerial")
                                                       .counterBuilder("serial.bytes_read")
                                                       .setDescription("Total bytes read from serial ports")
                                                       .setUnit("bytes")
                                                       .build();
      }
    }
    catch (Throwable ignore)
    {
    }
  }

  /**
   * Records bytes written to a serial port for telemetry.
   *
   * @param bytes    the number of bytes written
   * @param portName the name of the serial port
   */
  public static void addBytesWritten(long bytes, String portName)
  {
    if (bytesWrittenCounter == null)
    {
      initializeMetrics();
    }
    if (bytesWrittenCounter instanceof LongCounter)
    {
      ((LongCounter) bytesWrittenCounter).add(bytes, Attributes.of(AttributeKey.stringKey("serial.port"), portName));
    }
  }

  /**
   * Records bytes read from a serial port for telemetry.
   *
   * @param bytes    the number of bytes read
   * @param portName the name of the serial port
   */
  public static void addBytesRead(long bytes, String portName)
  {
    if (bytesReadCounter == null)
    {
      initializeMetrics();
    }
    if (bytesReadCounter instanceof LongCounter)
    {
      ((LongCounter) bytesReadCounter).add(bytes, Attributes.of(AttributeKey.stringKey("serial.port"), portName));
    }
  }

  /**
   * The underlying jSerialComm port.
   */
  @JsonIgnore
  private com.fazecast.jSerialComm.SerialPort connectionPort;

  /**
   * Sets the underlying jSerialComm port.
   *
   * @param connectionPort the jSerialComm port
   * @return this instance for method chaining
   */
  public @org.jspecify.annotations.NonNull J setConnectionPort(SerialPort connectionPort)
  {
    this.connectionPort = connectionPort;
    return (J) this;
  }

  /**
   * Flag indicating if the output buffer is empty.
   */
  @JsonIgnore
  private final AtomicBoolean outputBufferEmpty = new AtomicBoolean(false);

  /**
   * Flag indicating if the port is clear to send data.
   */
  @JsonIgnore
  private final AtomicBoolean clearToSend = new AtomicBoolean(false);

  /**
   * The COM port number (e.g., 1 for COM1).
   */
  private Integer comPort;

  /**
   * The baud rate for the connection. Default is 9600.
   */
  private BaudRate baudRate = BaudRate.$9600;

  /**
   * The current status of the connection. Default is Offline.
   */
  private ComPortStatus comPortStatus = ComPortStatus.Offline;

  /**
   * The type of COM port. Default is Device.
   */
  private ComPortType comPortType = ComPortType.Device;

  /**
   * The number of data bits. Default is 8.
   */
  private DataBits dataBits = DataBits.$8;

  /**
   * The flow control method. Default is None.
   */
  private FlowControl flowControl = FlowControl.None;

  /**
   * The parity checking method. Default is None.
   */
  private Parity parity = Parity.None;

  /**
   * The number of stop bits. Default is 1.
   */
  private StopBits stopBits = StopBits.$1;

  /**
   * The flow type. Default is None.
   */
  private FlowType flow = FlowType.None;

  /**
   * The buffer size for reading data. Default is 1024 bytes.
   */
  private Integer bufferSize = 1024;

  /**
   * The number of seconds after which a connection is considered idle. Default is 120 seconds (2 minutes).
   */
  private Integer idleTimerSeconds = 120;


  /** The output stream for writing to the serial port. */
  @JsonIgnore
  private OutputStream writer = null;

  /** Callback invoked when the port status changes. */
  @JsonIgnore
  private BiConsumer<CerialPortConnection<?>, ComPortStatus> comPortStatusUpdate;

  /** Callback invoked when a port error occurs. */
  @JsonIgnore
  private TriConsumer<Throwable, CerialPortConnection<?>, ComPortStatus> comPortError;

  /** The idle monitor for this connection. */
  @JsonIgnore
  private CerialIdleMonitor monitor;

  /** The timestamp of the last received message. */
  private LocalDateTime lastMessageTime;

  /** The end-of-message delimiter characters. */
  @Setter
  @Getter
  private char[] endOfMessage = new char[]{'\n'};
		
  /**
   * Resets all connection parameters to their defaults.
   *
   * @return this connection for method chaining
   */
  public J reset()
  {
    baudRate = BaudRate.$9600;
    comPortStatus = ComPortStatus.Offline;
    dataBits = DataBits.$8;
    flowControl = FlowControl.None;
    parity = Parity.None;
    stopBits = StopBits.$1;
    flow = FlowType.None;
    Integer bufferSize = 1024;
    Integer idleTimerSeconds = 5;
    return (J) this;
  }

  /** Whether the connection is currently running. */
  private boolean run = false;

  /** The data listener attached to the serial port. */
  @JsonIgnore
  private SerialPortDataListener serialPortMessageListener;

  /** Timer ID for reconnect attempts, or {@code -1} if none. */
  @JsonIgnore
  private long reconnectTimerId = -1L;

  /** Number of reconnect attempts made. */
  @JsonIgnore
  private int reconnectAttempts = 0;

  /** Initial delay in seconds before a reconnect attempt. */
  private int initialReconnectDelaySeconds = 1;

  /** Maximum delay in seconds between reconnect attempts. */
  private int maxReconnectDelaySeconds = 60;

  /**
   * Creates a new serial port connection with the specified parameters and idle timeout.
   *
   * @param comPort  the COM port number (e.g., 1 for COM1)
   * @param baudRate the baud rate for the connection
   * @param seconds  the number of seconds after which the connection is considered idle
   */
  public CerialPortConnection(int comPort, BaudRate baudRate, int seconds)
  {
    this.comPort = comPort;
    this.baudRate = baudRate;
    reset();
    setComPortType(ComPortType.Device);

    connectionPort = com.fazecast.jSerialComm.SerialPort.getCommPort(getComPortName());
    serialPortMessageListener = new DataSerialPortMessageListener(endOfMessage, connectionPort, this);
    connectionPort.setBaudRate(baudRate.toInt());
    this.idleTimerSeconds = seconds;
    this.setMonitor(new CerialIdleMonitor(this, 2, 120, seconds));
    CerialConnectionRegistry.register(this);
    CerialPortConnection me = this;
    IGuiceContext.getAllLoadedServices()
        .computeIfAbsent(IGuicePreDestroy.class, k -> new TreeSet<>());
    Set set = IGuiceContext.getAllLoadedServices()
        .get(IGuicePreDestroy.class);
    set.add(me);
  }

  /**
   * Sets the COM port number.
   *
   * @param comPort the COM port number (e.g., 1 for COM1)
   * @return this instance for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPort(int comPort)
  {
    this.comPort = comPort;
    return (J) this;
  }

  /**
   * Gets the logger for this connection.
   * <p>
   * If the logger doesn't exist yet, it is created with a specific format and file name
   * based on the COM port number.
   *
   * @return the logger for this connection
   */
  public org.apache.logging.log4j.core.Logger getLog()
  {
    if (log == null)
    {
      String loggerName = (comPort == 0) ? "cerial" : "COM" + comPort;
      log = LogUtils.getSpecificRollingLogger(loggerName, "cerial",
          "[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%-5level] - [%msg]%n", false);
    }
    return log;
  }

  /**
   * Creates a new serial port connection with the specified parameters and a default idle timeout of 120 seconds.
   *
   * @param comPort  the COM port number (e.g., 1 for COM1)
   * @param baudRate the baud rate for the connection
   */
  public CerialPortConnection(int comPort, BaudRate baudRate)
  {
    this(comPort, baudRate, 120);
  }

  /**
   * Gets the idle monitor for this connection.
   * <p>
   * If the monitor doesn't exist yet, it is created with default settings.
   *
   * @return the idle monitor for this connection
   */
  public CerialIdleMonitor getMonitor()
  {
    if (monitor == null)
    {
      this.setMonitor(new CerialIdleMonitor(this, 2, 120, idleTimerSeconds));
    }
    return monitor;
  }

  /**
   * Connects to the serial port.
   * <p>
   * This method performs the following steps:
   * <ol>
   *   <li>Calls {@link #beforeConnect()} to prepare the connection</li>
   *   <li>Opens the port</li>
   *   <li>If successful, calls {@link #afterConnect()} to set up listeners and monitoring</li>
   *   <li>Registers a shutdown hook for proper cleanup</li>
   *   <li>Sets the connection status to {@link ComPortStatus#Silent}</li>
   * </ol>
   * <p>
   * If the connection fails, the status is set to {@link ComPortStatus#Missing} or
   * {@link ComPortStatus#GeneralException} depending on the error.
   *
   * @return this instance for method chaining
   */
  @Trace
  @SpanAttribute("connection_status")
  public J connect()
  {
    if (getConnectionPort() != null && getConnectionPort().isOpen())
    {
      return (J) this;
    }
    beforeConnect();
    try
    {
      getLog().info("🚀 Opening serial port '{}' at {} baud", getComPortName(), getBaudRate().toInt());
      connectionPort.setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
      connectionPort.openPort();
      if (connectionPort.isOpen())
      {
        afterConnect();
        registerShutdownHook();
        setComPortStatus(Silent);
        getLog().info("✅ Serial port connected: '{}'", getComPortName());
      }
      else
      {
        setComPortStatus(Missing);
      }
    }
    catch (Throwable e)
    {
      getLog().error("❌ Failed to open serial port '{}': {}", getComPortName(), e.getMessage(), e);
      onConnectError(e, ComPortStatus.GeneralException);
    }
    return (J) this;
  }

  /**
   * Disconnects from the serial port.
   * <p>
   * This method closes the port if it is open and sets the connection status to
   * {@link ComPortStatus#Offline}.
   *
   * @return this instance for method chaining
   */
  @Trace
  @SpanAttribute("connection_status")
  public J disconnect()
  {
    if (connectionPort != null && connectionPort.isOpen())
    {
      getLog().info("⚠️ Disconnecting serial port '{}'", getComPortName());
      connectionPort.closePort();
      setComPortStatus(Offline);
      getLog().info("✅ Serial port disconnected: '{}'", getComPortName());
    }
    return (J) this;
  }

  private int computeNextDelaySeconds()
  {
    // exponential backoff: 1,2,4,8,... capped at maxReconnectDelaySeconds
    int pow;
    try
    {
      pow = Math.multiplyExact(1, 1 << Math.min(reconnectAttempts, 16));
    }
    catch (ArithmeticException ex)
    {
      pow = Integer.MAX_VALUE;
    }
    int candidate = Math.max(1, initialReconnectDelaySeconds) * pow;
    int delay = Math.min(candidate, Math.max(1, maxReconnectDelaySeconds));
    reconnectAttempts++;
    return delay;
  }

  private void cancelReconnectTimer()
  {
    if (reconnectTimerId != -1L)
    {
      try
      {
        IGuiceContext.get(Vertx.class).cancelTimer(reconnectTimerId);
      }
      catch (Throwable ignore)
      {
        // ignore
      }
      reconnectTimerId = -1L;
    }
  }

  private void resetReconnectBackoff()
  {
    reconnectAttempts = 0;
    cancelReconnectTimer();
  }

  /**
   * Schedules a reconnect attempt with exponential backoff.
   *
   * @param reason the reason for reconnecting
   * @return this connection for method chaining
   */
  protected J scheduleReconnect(String reason)
  {
    if (connectionPort != null && connectionPort.isOpen())
    {
      // Already connected
      return (J) this;
    }
    if (reconnectTimerId != -1L)
    {
      // A reconnect attempt is already scheduled; avoid frenzy
      getLog().debug("⏳ Reconnect already scheduled for '{}' (timerId={})", getComPortName(), reconnectTimerId);
      return (J) this;
    }
    int delaySeconds = computeNextDelaySeconds();
    getLog().warn("🔄 Scheduling reconnect for '{}' in {}s (attempt {}) - Reason: {}", getComPortName(), delaySeconds, reconnectAttempts, reason);
    Vertx vertx = IGuiceContext.get(Vertx.class);
    reconnectTimerId = vertx.setTimer(delaySeconds * 1000L, id -> {
      reconnectTimerId = -1L;
      try
      {
        getLog().info("🔌 Attempting reconnect to '{}' (attempt {})", getComPortName(), reconnectAttempts);
        connect();
        if (connectionPort != null && connectionPort.isOpen())
        {
          getLog().info("✅ Reconnected to '{}'", getComPortName());
          resetReconnectBackoff();
        }
        else
        {
          // Not yet connected; schedule next attempt
          scheduleReconnect("Port not open after connect() attempt");
        }
      }
      catch (Throwable t)
      {
        onConnectError(t, ComPortStatus.GeneralException);
      }
    });
    return (J) this;
  }

  /**
   * Prepares the connection before opening the serial port.
   *
   * @return this connection for method chaining
   */
  public J beforeConnect()
  {
      IGuiceContext.instance()
          .inject()
          .injectMembers(this)
      ;
    getLog().debug("📋 Preparing connection - Port: '{}', Baud: {}, DataBits: {}, Parity: {}, StopBits: {}, Flow: {}",
        getComPortName(), getBaudRate().toInt(), getDataBits(), getParity(), getStopBits(), flow);
    configure(connectionPort);
    return (J) this;
  }

  /**
   * Performs post-connection setup including data listener registration and idle monitoring.
   *
   * @return this connection for method chaining
   */
  public J afterConnect()
  {
    setComPortStatus(Silent);
    getLog().debug("📋 Post-connect setup for '{}'", getComPortName());
    connectionPort.removeDataListener();
    connectionPort.addDataListener(serialPortMessageListener);
    if (flow != null && flow != FlowType.None)
    {
      switch (flow)
      {
        case XONXOFF:
          setXOnXOff();
          break;
        case RTSCTS:
          setRts();
          break;
      }
    }
    getLog().debug("📊 Starting idle monitor ({}s) for '{}'", idleTimerSeconds, getComPortName());
    getMonitor().begin();
    // Successful connect -> reset backoff and cancel any pending reconnect timers
    resetReconnectBackoff();
    return (J) this;
  }

  /**
   * Configures XON/XOFF flow control on the serial port.
   *
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setXOnXOff()
  {
    connectionPort.setFlowControl(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED);
    getLog().debug("📋 Configured flow control: XON/XOFF for '{}'", getComPortName());
    return (J) this;
  }

  /**
   * Configures RTS/CTS hardware flow control on the serial port.
   *
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setRts()
  {
    connectionPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
    getLog().debug("📋 Configured flow control: RTS/CTS for '{}'", getComPortName());
    return (J) this;
  }


  /**
   * Configures DSR/DTR hardware flow control on the serial port.
   *
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setDsr()
  {
    connectionPort.setFlowControl(SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED);
    getLog().debug("📋 Configured flow control: DSR/DTR for '{}'", getComPortName());
    return (J) this;
  }


  /**
   * Handles a connection error by notifying the error callback, disconnecting, and scheduling reconnect.
   *
   * @param e      the exception that occurred
   * @param status the resulting port status
   * @return this connection for method chaining
   */
  public J onConnectError(Throwable e, ComPortStatus status)
  {
       getLog().error("❌ Error on '{}': {}", getComPortName(), e.getMessage(), e);
    if (comPortError != null)
    {
      comPortError.accept(e, this, status);
    }
    else
    {
      setComPortStatus(status);
    }
    disconnect();
    if (monitor != null)
    {
      monitor.end();
    }
    // Schedule reconnect with backoff
    scheduleReconnect(e != null ? String.valueOf(e.getMessage()) : String.valueOf(status));
    return (J) this;
  }

  /**
   * Registers a callback for port status changes.
   *
   * @param comPortStatusUpdate the callback to invoke on status change
   * @return this connection for method chaining
   */
  public J onComPortStatusUpdate(BiConsumer<CerialPortConnection<?>, ComPortStatus> comPortStatusUpdate)
  {
    this.comPortStatusUpdate = comPortStatusUpdate;
    return (J) this;
  }

  /**
   * Sets the port status and optionally notifies the status update callback.
   *
   * @param comPortStatus the new port status
   * @param update        if empty or first element is {@code true}, the callback is notified
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortStatus(ComPortStatus comPortStatus, boolean... update)
  {
    if (this.comPortStatus != comPortStatus && (
        (update != null && update.length == 0) ||
            (update != null && update[0]))
    )
    {
      if (this.comPortStatusUpdate != null)
      {
        this.comPortStatusUpdate.accept(this, comPortStatus);
      }
    }
    this.comPortStatus = comPortStatus;
    return (J) this;
  }

  /**
   * Registers this connection for cleanup during application shutdown.
   *
   * @return this connection for method chaining
   */
  protected J registerShutdownHook()
  {
    IGuiceContext.instance()
        .loadPreDestroyServices()
        .add(this)
    ;
    getLog().debug("📋 Registered shutdown hook for '{}'", getComPortName());
    return (J) this;
  }

  /**
   * Called after the serial port has been shut down.
   *
   * @return this connection for method chaining
   */
  protected J afterShutdown()
  {
    getLog().info("🌟 Shutdown completed for '{}'", getComPortName());
    return (J) this;
  }

  /**
   * Called before the serial port is shut down; stops the idle monitor.
   *
   * @return this connection for method chaining
   */
  protected J beforeShutdown()
  {
    getLog().info("⚠️ Shutting down serial port '{}'", getComPortName());
    getMonitor().end();
    return (J) this;
  }

  /**
   * Hook for subclasses to apply custom configuration to the serial port.
   *
   * @param instance the serial port instance to configure
   * @return this connection for method chaining
   */
  protected J configure(SerialPort instance)
  {
    return (J) this;
  }

  private J me()
  {
    return (J) this;
  }

  /**
   * Returns the platform-specific COM port name (e.g., "COM1" on Windows, "/dev/ttyUSB0" on Linux).
   *
   * @return the COM port name
   */
  @JsonProperty
  public String getComPortName()
  {
    String name;
    if (OSValidator.isWindows())
    {
      name = "COM" + getComPort();
    }
    else
    {
      name = "/dev/ttyUSB" + getComPort() + " " + getBaudRate().toInt();
    }
    if (getLog() != null && getLog().isDebugEnabled())
    {
      getLog().trace("📝 Computed port name: '{}'", name);
    }
    return name;
  }

  /**
   * Writes a message to the serial port.
   * <p>
   * This method sends the specified message to the serial port. If the message doesn't
   * end with a newline character, one is automatically added. The message is logged
   * with the COM port number and the message content.
   * <p>
   * If the port is not open, the message is not sent and a trace log entry is made.
   *
   * @param message                the message to send
   * @param checkForEndOfCharacter optional parameter (not used in the current implementation)
   * @throws RuntimeException if an error occurs while writing to the port (wrapped by @SneakyThrows)
   */
  public void write(String message, boolean... checkForEndOfCharacter)
  {
    if (connectionPort != null && connectionPort.isOpen())
    {
      if (!Strings.isNullOrEmpty(message))
      {
        if (!message.endsWith(String.valueOf('\n')))
        {
          message += '\n';
        }
        try
        {
          if (IGuiceContext.instance().getScanResult().getClassesImplementing(com.guicedee.client.services.lifecycle.IGuiceModule.class).loadClasses().stream().anyMatch(c -> c.getSimpleName().equals("TraceModule")))
          {
            IGuiceContext.get(com.guicedee.cerial.implementations.CerialWriteTracer.class).onWrite(message, getComPortName(), getComPort());
          }
          byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
          connectionPort.writeBytes(bytes, bytes.length);
          getLog().info("📤 TX - Port {} - Message: {}", portNumberFormat.format(getComPort()), message.trim());
          addBytesWritten(bytes.length, getComPortName());
        }
        catch (Throwable t)
        {
          getLog().error("❌ Write failed on '{}': {}", getComPortName(), t.getMessage(), t);
          onConnectError(t, ComPortStatus.GeneralException);
        }
      }
      //log.warn("TX : {}", message);
    }
    else
    {
      getLog().trace("⚠️ Message NOT sent - Port not open - Message: [{}]", message);
    }
  }

  /**
   * Cleans up resources when the object is destroyed.
   * <p>
   * This method is called by the GuicedInjection framework during application shutdown.
   * It disconnects from the serial port if it is open, ensuring proper resource cleanup.
   * <p>
   * This implementation of the {@link IGuicePreDestroy} interface ensures that serial
   * port connections are properly closed when the application shuts down.
   */
  @Override
  public void onDestroy()
  {
    getLog().info("⚠️ onDestroy invoked for '{}'", getComPortName());
    CerialConnectionRegistry.unregister(this);
    if (connectionPort != null)
    {
      if (connectionPort.isOpen())
      {
        disconnect();
      }
    }
  }

  @SuppressWarnings("unused")
  private static class OSValidator
  {
    private static final String OS = System.getProperty("os.name")
                                         .toLowerCase();

    public static boolean isWindows()
    {
      return OS.contains("win");
    }

    public static boolean isMac()
    {
      return OS.contains("mac");
    }

    public static boolean isUnix()
    {
      return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    }

    public static boolean isSolaris()
    {
      return OS.contains("sunos");
    }
  }

  /**
   * Sets the COM port number.
   *
   * @param comPort the COM port number
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPort(Integer comPort)
  {
    this.comPort = comPort;
    return (J) this;
  }

  /**
   * Sets the baud rate.
   *
   * @param baudRate the baud rate
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setBaudRate(BaudRate baudRate)
  {
    this.baudRate = baudRate;
    return (J) this;
  }

  /**
   * Sets the port status and notifies the status update callback if changed.
   *
   * @param comPortStatus the new port status
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortStatus(ComPortStatus comPortStatus)
  {
    if (this.comPortStatus != comPortStatus && this.comPortStatusUpdate != null)
    {
      getLog().debug("🔄 Updating port status: Port [{}] changing to [{}]", comPort, comPortStatus);
      this.comPortStatusUpdate.accept(this, comPortStatus);
    }
    this.comPortStatus = comPortStatus;
    return (J) this;
  }

  /**
   * Sets the COM port type.
   *
   * @param comPortType the port type
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortType(ComPortType comPortType)
  {
    this.comPortType = comPortType;
    return (J) this;
  }

  /**
   * Sets the data bits configuration.
   *
   * @param dataBits the data bits
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setDataBits(DataBits dataBits)
  {
    this.dataBits = dataBits;
    return (J) this;
  }

  /**
   * Sets the flow control mode.
   *
   * @param flowControl the flow control mode
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setFlowControl(FlowControl flowControl)
  {
    this.flowControl = flowControl;
    return (J) this;
  }

  /**
   * Sets the parity mode.
   *
   * @param parity the parity mode
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setParity(Parity parity)
  {
    this.parity = parity;
    return (J) this;
  }

  /**
   * Sets the stop bits configuration.
   *
   * @param stopBits the stop bits
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setStopBits(StopBits stopBits)
  {
    this.stopBits = stopBits;
    return (J) this;
  }

  /**
   * Sets the read buffer size.
   *
   * @param bufferSize the buffer size in bytes
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setBufferSize(Integer bufferSize)
  {
    this.bufferSize = bufferSize;
    return (J) this;
  }

  /**
   * Sets the output stream writer for the serial port.
   *
   * @param writer the output stream
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setWriter(OutputStream writer)
  {
    this.writer = writer;
    return (J) this;
  }

  /**
   * Sets the callback for port status changes.
   *
   * @param comPortStatusUpdate the status update callback
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortStatusUpdate(BiConsumer<CerialPortConnection<?>, ComPortStatus> comPortStatusUpdate)
  {
    this.comPortStatusUpdate = comPortStatusUpdate;
    return (J) this;
  }

  /**
   * Sets the callback for data read events.
   *
   * @param comPortRead the data read callback
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortRead(BiConsumer<byte[], com.fazecast.jSerialComm.SerialPort> comPortRead)
  {
    if (this.serialPortMessageListener == null)
    {
      getLog().warn("⚠️ Port '{}' not yet ready to register read listener", getComPortName());
      return (J) this;
    }
    ((ComPortEvents) serialPortMessageListener).setComPortRead(comPortRead);
    getLog().debug("🔗 Read listener registered for '{}'", getComPortName());
    return (J) this;
  }

  /**
   * Sets the callback for port errors.
   *
   * @param comPortError the error callback
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setComPortError(TriConsumer<Throwable, CerialPortConnection<?>, ComPortStatus> comPortError)
  {
    this.comPortError = comPortError;
    return (J) this;
  }

  /**
   * Sets the idle monitor for this connection.
   *
   * @param monitor the idle monitor
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setMonitor(CerialIdleMonitor monitor)
  {
    this.monitor = monitor;
    return (J) this;
  }

  /**
   * Sets the timestamp of the last received message.
   *
   * @param lastMessageTime the last message timestamp
   * @return this connection for method chaining
   */
  public @org.jspecify.annotations.NonNull J setLastMessageTime(LocalDateTime lastMessageTime)
  {
    this.lastMessageTime = lastMessageTime;
    return (J) this;
  }
}
