# Sequence — Idle Monitor Updates Silent Status

```mermaid
sequenceDiagram
    title Idle monitor cycle
    participant Monitor as CerialIdleMonitor
    participant Vertx as Vert.x 5
    participant Connection as CerialPortConnection
    participant Events as ComPortEvents

    Monitor->>Vertx: setPeriodic(timerId)
    Vertx-->>Monitor: tick(period)
    Monitor->>Connection: fetch `lastMessageTime` and `comPortStatus`
    alt Idle threshold exceeded
        Monitor->>Connection: setComPortStatus(ComPortStatus.Silent)
        Connection->>Events: onStatusChanged(Silent)
    else Active
        Monitor->>Events: no change (log sampled)
    end
```

Notes
- `CerialIdleMonitor` uses `IGuiceContext.get(Vertx.class)` at runtime (see `CerialIdleMonitor.begin()`).
- When a connection is marked `ComPortStatus.Silent`, `ComPortEvents` propagates the transition to any registered consumers.
