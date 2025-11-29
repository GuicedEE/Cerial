# C4 Component — Serial I/O Bounded Context

```mermaid
C4Component
    title Serial I/O Components
    Container(serialApi, "Serial API", "Java", "Fluent API exposed to consumer code")
    Component(connection, "CerialPortConnection", "CRTP fluent manager", "Configures `SerialPort`, tracks status, exposes `write`, `connect`, `disconnect`, and `setComPortRead` hooks.")
    Component(events, "ComPortEvents", "Status/Data callback aggregator", "Dispatches connection status changes and data events to supplied consumers and logs diagnostics.")
    Component(provider, "CerialPortConnectionProvider", "Guice provider", "Creates and configures `CerialPortConnection` instances on-demand for each port number.")
    Component(idleMonitor, "CerialIdleMonitor", "Vert.x-backed idle detection", "Schedules timers that evaluate the last message timestamp and update `ComPortStatus` to `Silent` when idle.")
    Component(dataListeners, "DataSerialPort*Listener", "jSerialComm adapters", "Connects `SerialPortDataListener` events to the library’s callback hooks and `ComPortEvents`.")
    Rel(connection, events, "Notifies on status/data updates via `ComPortEvents`")
    Rel(idleMonitor, connection, "Reads `lastMessageTime` and calls `setComPortStatus`")
    Rel(connection, dataListeners, "Registers `SerialPortDataListener` implementations on the `SerialPort`")
    Rel(provider, connection, "Supplies configured `CerialPortConnection` and ensures it is registered as a singleton per port")
```

## Component responsibilities

- **CerialPortConnection** (source: `src/main/java/com/guicedee/cerial/CerialPortConnection.java`): Core CRTP domain object that exposes fluent setters, connection lifecycle methods, data writers/readers, and implements `IGuicePreDestroy` for clean shutdown as part of the GuicedEE lifecycle.
- **ComPortEvents** (`src/main/java/com/guicedee/cerial/implementations/ComPortEvents.java`): Central logging/reporting helper that surfaces status updates, event callbacks, and produces `ComPortStatus` summaries for consumers.
- **CerialPortConnectionProvider** (`src/main/java/com/guicedee/cerial/implementations/CerialPortConnectionProvider.java`): Guice provider responsible for instantiating each port’s `CerialPortConnection` with default config, packaging the port number and connection name.
- **CerialIdleMonitor** (`src/main/java/com/guicedee/cerial/CerialIdleMonitor.java`): Uses `IGuiceContext.get(Vertx.class)` to start/stop periodic timers that detect idle connections and update statuses.
- **DataSerialPortMessageListener` / `DataSerialPortBytesListener`** (`src/main/java/com/guicedee/cerial/implementations/`): Replace raw jSerialComm listeners with GuicedCerial-friendly callbacks that drive `ComPortEvents` and update the connection’s last message time.
