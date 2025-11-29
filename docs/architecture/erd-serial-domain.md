# ERD — Serial Domain Objects

```mermaid
classDiagram
    class CerialPortConnection {
        Integer comPort
        BaudRate baudRate
        DataBits dataBits
        Parity parity
        StopBits stopBits
        FlowControl flowControl
        FlowType flow
        ComPortStatus comPortStatus
        LocalDateTime lastMessageTime
    }
    class CerialIdleMonitor {
        Integer seconds
        Integer period
        Integer initialDelay
        ComPortStatus previousStatus
    }
    class ComPortEvents {
        BiConsumer<CerialPortConnection<?>, ComPortStatus> statusConsumer
        BiConsumer<CerialPortConnection<?>, byte[]> dataConsumer
    }
    CerialIdleMonitor --> CerialPortConnection : observes
    CerialPortConnection --> ComPortEvents : emits
```

This diagram captures the core domain objects defined in `com.guicedee.cerial`. The `CerialPortConnection` stores every serial-port configuration parameter and emits events through `ComPortEvents`. `CerialIdleMonitor` references the connection to evaluate `lastMessageTime` and transition `ComPortStatus` when necessary.  
