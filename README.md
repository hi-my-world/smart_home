# Smart Home Device Management System — Adapter Pattern

## Project Overview

This project is a small Java application that controls several different
"smart home" devices — an air conditioner, a fan, a light, and a heater —
through a single, unified interface, even though each device's underlying
(legacy) API is completely different and incompatible with the others.

It demonstrates the **Adapter Design Pattern**.

## The Problem

Imagine four legacy/third-party device libraries already exist:

- `LegacyAirConditioner` — `startCooling()`, `stopCooling()`, `setTemperature(int)`, `getACState()`
- `LegacyFan` — `startFan()`, `stopFan()`, `changeSpeed(String)`, `checkFanStatus()`
- `LegacyLight` — `enableLight()`, `disableLight()`, `setBrightness(int)`, `lightStatus()`
- `LegacyHeater` — `startHeating()`, `stopHeating()`, `setHeatLevel(int)`, `readHeaterStatus()`

Each one uses different method names, and each one represents its
intensity/level with a different unit (Celsius, a speed word, a
percentage, watts). A `SmartHomeController` that wants to manage all of
them uniformly cannot call these APIs directly without hard-coding a
branch for every device type.

We also cannot modify the legacy classes — in a real scenario, we may
not own that source code, and changing it would break other systems
that depend on it.

## Why the Adapter Pattern Is Appropriate

The Adapter Pattern lets us introduce a small wrapper class per legacy
device that translates calls from a common `SmartDevice` interface into
whatever the legacy device actually understands, without touching the
legacy code itself. The client (`SmartHomeController`) is then written
once, against the common interface, and works with any device that has
an adapter — current or future.

## Adapter Pattern Roles

| Role | Class(es) |
|---|---|
| **Target** | `SmartDevice` |
| **Client** | `SmartHomeController` |
| **Adapters** | `AirConditionerAdapter`, `FanAdapter`, `LightAdapter`, `HeaterAdapter` |
| **Adaptees** | `LegacyAirConditioner`, `LegacyFan`, `LegacyLight`, `LegacyHeater` |

### Target — `SmartDevice`

```java
public interface SmartDevice {
    void turnOn();
    void turnOff();
    void setLevel(int level);   // 1 (lowest) .. 10 (highest)
    String getStatus();
}
```

### Client — `SmartHomeController`

Holds a `List<SmartDevice>` and exposes `addDevice`, `removeDevice`,
`turnOnAll`, `turnOffAll`, `setLevelAll`, and `showAllStatus`. It only
ever calls methods on `SmartDevice` — it has no `import` of, or
`instanceof` check against, any legacy or adapter class.

### Adapters

Each adapter implements `SmartDevice`, wraps one legacy device instance,
and converts both the method calls and the data values:

| Adapter | Wraps | `turnOn()` calls | `turnOff()` calls | `getStatus()` calls |
|---|---|---|---|---|
| `AirConditionerAdapter` | `LegacyAirConditioner` | `startCooling()` | `stopCooling()` | `getACState()` |
| `FanAdapter` | `LegacyFan` | `startFan()` | `stopFan()` | `checkFanStatus()` |
| `LightAdapter` | `LegacyLight` | `enableLight()` | `disableLight()` | `lightStatus()` |
| `HeaterAdapter` | `LegacyHeater` | `startHeating()` | `stopHeating()` | `readHeaterStatus()` |

## Data Conversion Examples

Every adapter validates that `level` is between 1 and 10 (inclusive) and
throws `IllegalArgumentException` otherwise, then converts it into the
legacy device's own representation:

| Device | Conversion formula | Level 1 | Level 5 | Level 10 |
|---|---|---|---|---|
| Air Conditioner | `temp = 30 - (level-1) * 14/9` (°C) | 30°C | 24°C | 16°C |
| Fan | 1–3→LOW, 4–7→MEDIUM, 8–10→HIGH | LOW | MEDIUM | HIGH |
| Light | `brightness = level * 10` (%) | 10% | 50% | 100% |
| Heater | `wattage = level * 100` (W) | 100W | 500W | 1000W |

A higher application level always means a "stronger" effect (colder air,
faster fan, brighter light, more heat), even though the underlying units
differ completely.

## Project Structure

```
smart-home-adapter/
├── README.md
└── src/
    └── smarthome/
        ├── Main.java
        ├── SmartDevice.java
        ├── SmartHomeController.java
        ├── adapter/
        │   ├── AirConditionerAdapter.java
        │   ├── FanAdapter.java
        │   ├── LightAdapter.java
        │   └── HeaterAdapter.java
        └── legacy/
            ├── LegacyAirConditioner.java
            ├── LegacyFan.java
            ├── LegacyLight.java
            └── LegacyHeater.java
```

## How to Compile and Run

From the `smart-home-adapter` directory:

```bash
javac -d out $(find src -name "*.java")
java -cp out smarthome.Main
```

(Any JDK 11+ works. If `java`/`javac` are not on your PATH, use the full
path to a JDK's `bin` directory instead, e.g.
`/opt/homebrew/opt/openjdk/bin/javac`.)

## Example Output

```
===== SMART HOME SYSTEM =====

Adding devices...

Turning on all devices...

[LegacyAirConditioner] Cooling started.
[LegacyFan] Fan started.
[LegacyLight] Light enabled.
[LegacyHeater] Heating started.

===== SETTING LEVEL TO 5 =====

AC Adapter: Level 5 -> Temperature 24C
[LegacyAirConditioner] Temperature set to 24C.
Fan Adapter: Level 5 -> MEDIUM
[LegacyFan] Speed changed to MEDIUM.
Light Adapter: Level 5 -> 50%
[LegacyLight] Brightness set to 50%.
Heater Adapter: Level 5 -> 500W
[LegacyHeater] Heat level set to 500W.

===== DEVICE STATUS =====
Air Conditioner: ON | Temperature: 24C
Fan: ON | Speed: MEDIUM
Light: ON | Brightness: 50%
Heater: ON | Power: 500W

===== INDIVIDUAL LEVEL CHANGES =====

AC Adapter: Level 1 -> Temperature 30C
AC Adapter: Level 10 -> Temperature 16C
Fan Adapter: Level 8 -> HIGH
Light Adapter: Level 1 -> 10%
Heater Adapter: Level 10 -> 1000W

===== TESTING INVALID LEVELS =====

Rejected invalid level 0: Level must be between 1 and 10, got: 0
Rejected invalid level 11: Level must be between 1 and 10, got: 11

===== TURNING OFF =====

[LegacyAirConditioner] Cooling stopped.
[LegacyFan] Fan stopped.
[LegacyLight] Light disabled.
[LegacyHeater] Heating stopped.

===== FINAL DEVICE STATUS =====
Air Conditioner: OFF | Temperature: 16C
Fan: OFF | Speed: HIGH
Light: OFF | Brightness: 10%
Heater: OFF | Power: 1000W
```

## Why Adapter Instead of Direct Integration?

Without the Adapter Pattern, `SmartHomeController` would need to know
about every legacy class directly — for example branching on
`instanceof LegacyAirConditioner` and calling `setTemperature(...)`, then
`instanceof LegacyFan` and calling `changeSpeed(...)`, and so on. This
tightly couples the controller to each legacy API's method names and
data formats. Adding a new device type would require editing the
controller itself, and any change to a legacy API would ripple into the
controller's logic.

With the Adapter Pattern, `SmartHomeController` depends **only** on the
`SmartDevice` interface. All of the method-name translation and
unit/range conversion is isolated inside the adapters. Adding a new
legacy device — say, a `LegacySmartLock` — only requires writing a new
`SmartLockAdapter` class; the controller does not change at all. This
keeps the system open for extension but closed for modification, which
is the core benefit the Adapter Pattern provides here.
