# C4 Container — GuicedCerial Library

```mermaid
C4Container
    title GuicedCerial Containers
    System_Boundary(guicedCerial, "GuicedCerial module") {
        Container(serialApi, "Serial API", "Java", "CRTP-based API built on `CerialPortConnection`, enumerations, and `ComPortEvents` for callbacks")
        Container(lifecycleBindings, "Lifecycle Bindings", "GuicedInjection module", "`CerialPortsBindings` plus `CerialPortConnectionProvider` register per-port singletons")
        Container(monitoring, "Monitoring", "Vert.x timer jobs", "`CerialIdleMonitor` polls injected connections, updates status via Vert.x periodic timers")
    }
    Container_Ext(jSerialComm, "jSerialComm driver", "Native serial access", "Provides OS-level port IO")
    Container_Ext(vertx, "Vert.x 5 event loop", "Event loop", "Used indirectly via `IGuiceContext.get(Vertx.class)`")
    Container_Ext(guicedInjection, "GuicedInjection runtime", "GuicedInjection module", "Discovers `IGuiceModule` bindings such as `CerialPortsBindings`")

    Rel(serialApi, lifecycleBindings, "Exposes fluent connection instances that the bindings register")
    Rel(serialApi, jSerialComm, "Wraps `SerialPort` read/write operations")
    Rel(monitoring, serialApi, "Reads connection metrics and updates `ComPortStatus`")
    Rel(monitoring, vertx, "Schedules timers through Vert.x")
    Rel(lifecycleBindings, guicedInjection, "Implements `IGuiceModule` to wire the bindings")
```

## Container details

- **Serial API**: `CerialPortConnection` is the core CRTP type offering fluent configuration of `BaudRate`, `FlowControl`, `Parity`, etc. `ComPortEvents` funnels state changes to registered callbacks, while `DataSerialPortMessageListener` and `DataSerialPortBytesListener` wrap jSerialComm listeners.
- **Lifecycle Bindings**: `CerialPortsBindings` iterates over a fixed range of port numbers, delegating to `CerialPortConnectionProvider` to lazily instantiate each `CerialPortConnection` singleton. This binding is discovered by GuicedInjection during startup.
- **Monitoring**: `CerialIdleMonitor` obtains Vert.x from `IGuiceContext` to poll the last activity timestamp and escalate idle connections to `ComPortStatus#Silent`. It translates timer callbacks into status updates consumed by `ComPortEvents`.
