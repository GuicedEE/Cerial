# Sequence — Opening and Writing to a COM Port

```mermaid
sequenceDiagram
    title COM Port connect + write
    participant App as GuicedEE App
    participant Guice as GuicedInjection
    participant Provider as CerialPortConnectionProvider
    participant Connection as CerialPortConnection
    participant SerialPort as jSerialComm SerialPort

    App->>Guice: Inject @Named("1") CerialPortConnection
    Guice->>Provider: request port 1 instance
    Provider->>Connection: construct with defaults from enumerations
    Connection->>SerialPort: configure baud/parity/flow/stop bits
    Connection->>SerialPort: open() and register DataSerialPort*Listener
    Connection->>App: return configured connection
    App->>Connection: write("Hello")
    Connection->>SerialPort: writeBytes()
    SerialPort-->>Connection: bytes written callback
```

Notes
- GuicedInjection discovers `CerialPortsBindings` because it implements `IGuiceModule` (`src/main/java/com/guicedee/cerial/implementations/CerialPortsBindings.java`).
- `CerialPortConnection` configures port parameters using the CRTP setters in `src/main/java/com/guicedee/cerial/enumerations` and sends data via the `com.fazecast.jSerialComm.SerialPort` instance.
