# C4 Context — GuicedCerial

```mermaid
C4Context
    title GuicedCerial Context
    Person(app, "GuicedEE consumer", "An application that needs serial-port connectivity")
    System_Boundary(cerialBoundary, "GuicedCerial module") {
        System(guicedCerial, "GuicedCerial", "JPMS-aligned module exposing serial-port APIs and lifecycle hook implementations")
    }
    System_Ext(guicedInjection, "GuicedInjection runtime", "Discovers and loads IGuiceModule providers such as CerialPortsBindings")
    System_Ext(jSerialComm, "jSerialComm driver", "Native OS serial-port access library")
    System_Ext(vertx, "Vert.x 5 event loop", "Provided by GuicedEE to host async timers and event handling")

    Rel(app, guicedCerial, "Injects CerialPortConnection via Guice/IGuiceModule and uses the fluent API")
    Rel(guicedCerial, guicedInjection, "Registers CerialPortsBindings as an IGuiceModule provider")
    Rel(guicedCerial, jSerialComm, "Configures ports, writes bytes, and listens for data")
    Rel(guicedCerial, vertx, "Schedules idle monitoring and async event adapters")
```

## Narrative

GuicedCerial is the serial-port helper module within the GuicedEE ecosystem. It plugs into GuicedInjection via `CerialPortsBindings` so that `CerialPortConnection` instances can be injected by name. The module depends on the Vert.x event loop retrieved from `IGuiceContext` for timers (e.g., `CerialIdleMonitor`). Actual byte-level access is delegated to the `jSerialComm` driver, while callers interact through the CRTP-style `CerialPortConnection` API and the `ComPortEvents` handler.
