# GuicedEE Cerial

[![Build](https://github.com/GuicedEE/Cerial/actions/workflows/build.yml/badge.svg)](https://github.com/GuicedEE/Cerial/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.guicedee/cerial)](https://central.sonatype.com/artifact/com.guicedee/cerial)
[![Maven Snapshot](https://img.shields.io/nexus/s/com.guicedee/cerial?server=https%3A%2F%2Foss.sonatype.org&label=Maven%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/com/guicedee/cerial/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](https://www.apache.org/licenses/LICENSE-2.0)

![Java 25+](https://img.shields.io/badge/Java-25%2B-green)
![Guice 7](https://img.shields.io/badge/Guice-7%2B-green)
![Vert.X 5](https://img.shields.io/badge/Vert.x-5%2B-green)
![Maven 4](https://img.shields.io/badge/Maven-4%2B-green)

Lifecycle-aware **serial port connectivity** for [GuicedEE](https://github.com/GuicedEE) applications using **jSerialComm** and **Vert.x 5**.
Inject `@Named` `CerialPortConnection` singletons by port number, configure with CRTP-fluent setters, and let the framework handle connection lifecycle, idle monitoring, automatic reconnect with exponential backoff, MicroProfile Health reporting, and optional OpenTelemetry tracing.

Built on [jSerialComm](https://fazecast.github.io/jSerialComm/) · [Vert.x](https://vertx.io/) · [Google Guice](https://github.com/google/guice) · [MicroProfile Health](https://github.com/eclipse/microprofile-health) · JPMS module `com.guicedee.cerial` · Java 25+

## 📦 Installation

```xml
<dependency>
  <groupId>com.guicedee</groupId>
  <artifactId>cerial</artifactId>
</dependency>
```

<details>
<summary>Gradle (Kotlin DSL)</summary>

```kotlin
implementation("com.guicedee:cerial:2.0.0-RC7")
```
</details>

## ✨ Features

- **Guice-managed port singletons** — `CerialPortsBindings` pre-binds ports 0–999 as `@Named("0")` through `@Named("999")` singletons; inject by port number
- **CRTP-fluent configuration** — chain `setBaudRate()`, `setDataBits()`, `setParity()`, `setStopBits()`, `setFlowControl()` and call `connect()` — all methods return `this`
- **Idle monitoring** — `CerialIdleMonitor` uses a Vert.x periodic timer to detect silent connections and update status to `Silent`
- **Automatic reconnect** — exponential backoff reconnect (1 s → 60 s cap) via Vert.x timers on connection failure or port loss
- **Status lifecycle** — rich `ComPortStatus` enum with 12 states (`Offline`, `Silent`, `Running`, `Missing`, `Failed`, etc.) and grouped `EnumSet` helpers
- **Message-delimited reads** — `DataSerialPortMessageListener` uses jSerialComm's `SerialPortMessageListener` for delimiter-based framing (default: `\n`)
- **Byte-level reads** — `DataSerialPortBytesListener` for raw byte-array reception
- **Per-port logging** — each connection gets its own Log4j2 rolling file logger under `cerial/`
- **Health check** — `CerialHealthCheck` implements `@Liveness`, `@Readiness`, `@Startup` and reports status of all active connections via the [health](../health) module
- **Connection registry** — `CerialConnectionRegistry` tracks all active connections for health and lifecycle management
- **OpenTelemetry tracing** — optional `@Trace` spans on `connect()`, `disconnect()`, read, and write with `serial.port`, `serial.bytes_read`, and `serial.bytes_written` metrics (requires `guiced-telemetry`)
- **Cross-platform** — COM ports on Windows (`COM1`), USB serial on Linux (`/dev/ttyUSB0`)
- **JSpecify nullability** — `@NonNull` annotations on all public fluent setters
- **JSON serializable** — `CerialPortConnection` implements `IJsonRepresentation` with Jackson annotations
- **Graceful shutdown** — `IGuicePreDestroy` integration closes all open ports when the context tears down

## 🚀 Quick Start

**Step 1** — Inject a named connection by port number:

```java
@Inject
@Named("1")
private CerialPortConnection connection;
```

**Step 2** — Configure and connect:

```java
connection.setBaudRate(BaudRate.$9600)
          .setDataBits(DataBits.$8)
          .setParity(Parity.None)
          .setStopBits(StopBits.$1)
          .setFlowControl(FlowControl.None)
          .connect();
```

**Step 3** — Send and receive data:

```java
// Write
connection.write("Hello, device!");

// Read (callback on Vert.x worker thread)
connection.setComPortRead((data, port) -> {
    String message = new String(data).trim();
    System.out.println("Received: " + message);
});
```

**Step 4** — Monitor status changes:

```java
connection.onComPortStatusUpdate((conn, status) -> {
    System.out.println("Port " + conn.getComPort() + " → " + status);
});
```

No JPMS `provides` declarations are needed for consuming code — the module is registered automatically via its own `IGuiceModule` provider.

## 📐 Startup Flow

```
IGuiceContext.instance()
 └─ Guice injector created
     └─ CerialPortsBindings.configure()
         └─ Bind @Named("0")..@Named("999") → CerialPortConnectionProvider (Singleton)
 └─ First injection of @Named("N") CerialPortConnection
     └─ CerialPortConnectionProvider.get()
         ├─ new CerialPortConnection(N, BaudRate.$9600)
         ├─ SerialPort.getCommPort("COMN")
         ├─ DataSerialPortMessageListener created
         ├─ CerialIdleMonitor created
         ├─ CerialConnectionRegistry.register(this)
         └─ Register as IGuicePreDestroy
 └─ connection.connect()
     ├─ beforeConnect() → Guice injects members, configure port parameters
     ├─ SerialPort.openPort()
     ├─ afterConnect() → attach data listener, start idle monitor
     ├─ Register shutdown hook
     └─ Status → Silent
```

## 🔌 Connection Lifecycle

### Status states

`ComPortStatus` provides 12 states with UI metadata (icon, background/foreground classes):

| Status | Meaning | Group |
|---|---|---|
| `Offline` | Port is closed or disconnected | Exception |
| `Missing` | Port not detected on the system | Exception |
| `Failed` | Connection attempt failed | Exception |
| `InUse` | Port claimed by another process | Exception |
| `GeneralException` | Unhandled error | Exception |
| `Opening` | Port is initializing | Transitional |
| `OperationInProgress` | Long-running operation active | Transitional |
| `FileTransfer` | Bulk data transfer underway | Transitional |
| `Silent` | Connected but idle | Active |
| `Logging` | Connected and logging telemetry | Active |
| `Running` | Connected and actively communicating | Active |
| `Simulation` | Running in simulated mode | Active |

Grouped `EnumSet` helpers: `exceptionOperations`, `pauseOperations`, `portActive`, `portOffline`, `onlineServerStatus`.

### Reconnect with exponential backoff

When a connection fails or is lost, `scheduleReconnect()` uses a Vert.x timer with exponential backoff:

- Starts at `initialReconnectDelaySeconds` (default 1 s)
- Doubles each attempt: 1 s → 2 s → 4 s → 8 s → …
- Capped at `maxReconnectDelaySeconds` (default 60 s)
- Resets on successful reconnect

```java
connection.setInitialReconnectDelaySeconds(2)
          .setMaxReconnectDelaySeconds(30);
```

### Idle monitoring

`CerialIdleMonitor` runs a Vert.x periodic timer (default every 120 s) that checks `lastMessageTime`. If idle beyond the threshold, the status transitions to `Silent`:

```java
// Custom idle detection: check every 30s, idle after 60s
connection.setIdleTimerSeconds(60);
connection.setMonitor(new CerialIdleMonitor(connection, 2, 30, 60));
```

### Error handling

Register an error callback for connection failures:

```java
connection.setComPortError((throwable, conn, status) -> {
    log.error("Port {} error: {} → {}", conn.getComPort(), throwable.getMessage(), status);
});
```

If no error callback is set, the status is updated automatically and reconnect is scheduled.

## ⚙️ Configuration

### Serial port parameters

| Method | Default | Values |
|---|---|---|
| `setBaudRate()` | `BaudRate.$9600` | `$300`, `$600`, `$1200`, `$4800`, `$9600`, `$14400`, `$19200`, `$38400`, `$57600`, `$115200`, `$128000`, `$256000` |
| `setDataBits()` | `DataBits.$8` | `$5`, `$6`, `$7`, `$8` |
| `setParity()` | `Parity.None` | `None`, `Odd`, `Even`, `Mark`, `Space` |
| `setStopBits()` | `StopBits.$1` | `$1`, `$1_5`, `$2` |
| `setFlowControl()` | `FlowControl.None` | `None`, `RtsCtsIn`, `RtsCtsOut`, `XonXoffIn`, `XonXoffOut` |

### Flow type

Set an overall flow type that configures the underlying jSerialComm flags:

```java
connection.setFlow(FlowType.XONXOFF);  // or FlowType.RTSCTS, FlowType.None
```

### Message delimiter

```java
connection.setEndOfMessage(new char[]{'\r', '\n'});
```

### Buffer size

```java
connection.setBufferSize(2048);
```

## 🏥 Health Check

`CerialHealthCheck` is a `@Liveness`, `@Readiness`, `@Startup` MicroProfile Health check that reports the status of all active connections tracked by `CerialConnectionRegistry`:

```json
{
  "status": "UP",
  "checks": [{
    "id": "com.guicedee.cerial.CerialHealthCheck",
    "status": "UP",
    "data": {
      "COM1": "Running",
      "COM3": "Silent"
    }
  }]
}
```

A connection is **DOWN** if its status is `Offline`, `Missing`, `GeneralException`, `Failed`, or `InUse`. If no active connections exist, the check returns **UP** with `"No active connections"`.

## 📡 OpenTelemetry Integration

When `guiced-telemetry` is on the classpath, the module automatically:

- Creates `serial.bytes_written` and `serial.bytes_read` counters via the OpenTelemetry `Meter`
- Traces `connect()` and `disconnect()` calls with `@Trace` and `@SpanAttribute`
- Traces write operations via `CerialWriteTracer` with `serial.port`, `serial.portNumber`, `serial.message_length` span attributes
- Traces read operations via `CerialDataTracer` with `serial.data`, `serial.port`, `serial.message_length` span attributes

All telemetry dependencies are `requires static` — they are completely optional.

## 📝 Logging

Each connection creates a dedicated Log4j2 rolling file logger:

| Port | Logger name | File |
|---|---|---|
| COM1 | `COM1` | `cerial/COM1.log` |
| COM3 | `COM3` | `cerial/COM3.log` |
| (none) | `cerial` | `cerial/cerial.log` |

Log format: `[yyyy-MM-dd HH:mm:ss.SSS] [LEVEL] - [message]`

Messages use emoji prefixes for quick scanning:
- `📤 TX` — data sent
- `📥 RX` — data received
- `🚀` — port opening
- `✅` — success
- `❌` — error
- `⚠️` — disconnect/warning
- `🔄` — reconnect scheduled
- `🔌` — reconnect attempt

## 🗺️ Module Graph

```
com.guicedee.cerial
 ├── com.guicedee.guicedinjection   (GuicedEE runtime — scanning, Guice, lifecycle)
 ├── com.guicedee.client            (GuicedEE SPI contracts — IGuicePreDestroy, IGuiceModule)
 ├── com.guicedee.jsonrepresentation (JSON serialization — IJsonRepresentation)
 ├── com.fazecast.jSerialComm       (jSerialComm — serial port I/O)
 ├── io.vertx.core                  (Vert.x — timers for idle monitor and reconnect)
 ├── org.apache.commons.lang3       (Commons Lang — utility classes)
 ├── org.apache.commons.io          (Commons IO)
 ├── org.apache.logging.log4j       (Log4j2 — per-port rolling loggers)
 ├── com.guicedee.health            (optional — MicroProfile Health integration)
 └── com.guicedee.telemetry         (optional — OpenTelemetry tracing)
```

## 🧩 JPMS

Module name: **`com.guicedee.cerial`**

The module:
- **exports** `com.guicedee.cerial`, `com.guicedee.cerial.enumerations`, `com.guicedee.cerial.implementations`
- **provides** `IGuiceModule` with `CerialPortsBindings`
- **requires static** `com.guicedee.health` (optional health check integration)
- **requires static** `com.guicedee.telemetry` (optional OpenTelemetry tracing)

## 🏗️ Key Classes

| Class | Role |
|---|---|
| `CerialPortConnection<J>` | Core CRTP-fluent connection — configure, connect, write, receive, lifecycle |
| `CerialPortsBindings` | `IGuiceModule` — binds `@Named("0")`..`@Named("999")` to `CerialPortConnectionProvider` singletons |
| `CerialPortConnectionProvider` | Guice `Provider` — creates `CerialPortConnection` with default baud rate |
| `CerialConnectionRegistry` | Thread-safe registry of all active connections |
| `CerialIdleMonitor` | Vert.x periodic timer that detects idle connections |
| `CerialHealthCheck` | `@Liveness` + `@Readiness` + `@Startup` health check for all active connections |
| `CerialDataReceived` | Functional interface (`BiConsumer<byte[], CerialPortConnection>`) for read callbacks |
| `DataSerialPortMessageListener` | jSerialComm `SerialPortMessageListener` — delimiter-based message framing |
| `DataSerialPortBytesListener` | jSerialComm `SerialPortDataListener` — raw byte-array reception |
| `ComPortEvents` | SPI contract for read callbacks shared by message and byte listeners |
| `CerialDataTracer` | OpenTelemetry `@Trace` wrapper for read operations |
| `CerialWriteTracer` | OpenTelemetry `@Trace` wrapper for write operations |
| `SerialPortException` | `RuntimeException` for serial port errors |

### Enumerations

| Enum | Values |
|---|---|
| `BaudRate` | `$300` – `$256000` (12 rates) |
| `DataBits` | `$5`, `$6`, `$7`, `$8` |
| `Parity` | `None`, `Odd`, `Even`, `Mark`, `Space` |
| `StopBits` | `$1`, `$1_5`, `$2` |
| `FlowControl` | `None`, `RtsCtsIn`, `RtsCtsOut`, `XonXoffIn`, `XonXoffOut` |
| `FlowType` | `None`, `XONXOFF`, `RTSCTS` |
| `ComPortStatus` | 12 states with UI metadata (icon, CSS classes) |
| `ComPortType` | `Device`, etc. |

## 🤝 Contributing

Issues and pull requests are welcome — please include the serial port, OS, jSerialComm version, and log excerpt. Follow CRTP (no builders) and JSpecify nullness conventions.

## 📄 License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
