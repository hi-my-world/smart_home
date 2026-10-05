# Code Walkthrough — Smart Home Adapter Project

This document explains, file by file, what each class does and how they
fit together. For the high-level pattern explanation and conversion
tables, see [README.md](README.md).

---

## 1. `SmartDevice.java` (the Target)

```java
public interface SmartDevice {
    void turnOn();
    void turnOff();
    void setLevel(int level);
    String getStatus();
}
```

This is the **only** type the rest of the application is allowed to
depend on when it wants to control a device. It doesn't care whether
the real device is an air conditioner, a fan, a light, or a heater —
anything that implements these four methods can be plugged in.

---

## 2. The `legacy/` package (the Adaptees)

These four classes simulate "existing code you can't change" — think of
them as a third-party library. Each one is self-contained, has its own
method names, and its own way of representing intensity:

- **`LegacyAirConditioner`** — stores a boolean `cooling` flag and an
  `int temperature` (16–30). `setTemperature` throws if given a value
  outside that range.
- **`LegacyFan`** — stores a boolean `running` flag and a `String speed`
  that must be exactly `"LOW"`, `"MEDIUM"`, or `"HIGH"`.
- **`LegacyLight`** — stores a boolean `enabled` flag and an
  `int brightnessPercent` (0–100).
- **`LegacyHeater`** — stores a boolean `heating` flag and an
  `int wattage` (100–1000), which is a completely different unit from
  the other three devices on purpose, to prove the adapter can convert
  *any* representation.

None of these classes import or implement `SmartDevice`. That's
intentional — it's the whole point of the exercise. Each class also has
a `getXState()`-style method that builds a human-readable status string
from its own internal fields.

---

## 3. The `adapter/` package (the Adapters)

Each adapter is a small class that:

1. **Implements `SmartDevice`** — so it can be used anywhere a
   `SmartDevice` is expected.
2. **Holds a private reference to one legacy object** (passed in through
   the constructor — this is called "object composition," and it's how
   the Adapter Pattern is usually implemented in Java).
3. **Translates each interface method into the matching legacy call.**

Take `FanAdapter` as an example:

```java
public class FanAdapter implements SmartDevice {
    private final LegacyFan fan;

    public FanAdapter(LegacyFan fan) {
        this.fan = fan;
    }

    @Override
    public void turnOn() {
        fan.startFan();          // SmartDevice language -> Legacy language
    }

    @Override
    public void setLevel(int level) {
        validateLevel(level);
        String speed = /* 1-3 -> LOW, 4-7 -> MEDIUM, 8-10 -> HIGH */;
        fan.changeSpeed(speed);  // converts a number into the word the legacy class expects
    }
    ...
}
```

`turnOn()` on the adapter doesn't "do" anything by itself — it just
calls `fan.startFan()`. The interesting part is `setLevel(int level)`:
the caller passes a number from 1–10, but `LegacyFan` has no concept of
numbers, only the words `LOW`/`MEDIUM`/`HIGH`. The adapter is where that
translation happens.

The same idea repeats in the other three adapters, each with its own
math:

- `AirConditionerAdapter.setLevel` turns a 1–10 level into a Celsius
  value using `30 - (level - 1) * (14.0 / 9.0)`, then rounds it, because
  a *higher* level should mean *colder* air (so the formula counts
  downward from 30 to 16 as level goes up).
- `LightAdapter.setLevel` just multiplies by 10 (`level * 10`) to get a
  percentage.
- `HeaterAdapter.setLevel` multiplies by 100 (`level * 100`) to get
  watts.

Every adapter's `setLevel` calls a private `validateLevel(int level)`
helper first, which throws `IllegalArgumentException` if the value isn't
between 1 and 10. This validation lives in the adapter (not in the
legacy class and not in the controller) because 1–10 is *the
application's* contract — the legacy classes have their own, different
valid ranges (e.g. 16–30 for the AC) which they validate themselves.

`getStatus()` on each adapter simply forwards to the legacy class's own
status method (`getACState()`, `checkFanStatus()`, `lightStatus()`,
`readHeaterStatus()`) and returns that string unchanged — no conversion
needed there since it's just text for display.

---

## 4. `SmartHomeController.java` (the Client)

```java
public class SmartHomeController {
    private final List<SmartDevice> devices = new ArrayList<>();

    public void addDevice(SmartDevice device) { devices.add(device); }
    public void turnOnAll() {
        for (SmartDevice device : devices) {
            device.turnOn();
        }
    }
    ...
}
```

This class is intentionally "boring" — it only ever touches the
`SmartDevice` type. It has no `import smarthome.legacy.*;` anywhere in
the file, and there is no `if (device instanceof LegacyFan)` branching
logic. It doesn't know or care that an `AirConditionerAdapter` is
secretly wrapping a `LegacyAirConditioner` underneath — as far as this
class is concerned, every device in its list behaves identically.

This is the payoff of the pattern: because the controller is this
simple, adding a fifth device type later (say, a smart lock) requires
writing one new adapter class and nothing else — the controller doesn't
change at all.

---

## 5. `Main.java` (the demo / entry point)

`Main` is the only place where legacy classes, adapters, and the
controller are all mentioned in the same file — it's the "wiring" code.
It:

1. Constructs the four `Legacy*` objects directly (`new
   LegacyAirConditioner()`, etc.) — these represent the real,
   pre-existing hardware/library objects.
2. Wraps each one in its matching adapter and stores the result as a
   `SmartDevice` reference:
   ```java
   SmartDevice ac = new AirConditionerAdapter(legacyAC);
   ```
   From this line onward, the rest of `Main` (and the controller) only
   ever sees `ac` as a `SmartDevice` — the fact that it's secretly an
   `AirConditionerAdapter` wrapping a `LegacyAirConditioner` becomes an
   implementation detail.
3. Adds all four to a `SmartHomeController` and drives the whole demo:
   turn everything on, set a common level, print statuses, change
   individual levels, test invalid input (0 and 11), turn everything
   off, print final statuses.

The `tryInvalidLevel` helper at the bottom just wraps a `setLevel` call
in a try/catch so the program can show that out-of-range values are
rejected with a clear message instead of crashing or silently
corrupting state.

---

## How a single call flows through the system

Example: `controller.setLevelAll(5)` for the air conditioner specifically:

```
Main
  -> controller.setLevelAll(5)
       -> ac.setLevel(5)                         // ac is a SmartDevice reference
            -> AirConditionerAdapter.setLevel(5)  // actual runtime type
                 - validateLevel(5)               // passes, 1 <= 5 <= 10
                 - temperature = round(30 - 4*14/9) = 24
                 -> legacyAC.setTemperature(24)
                      - checks 16 <= 24 <= 30, OK
                      - this.temperature = 24
```

Notice the controller only ever wrote `device.setLevel(level)` — it has
no idea the number 5 eventually became the number 24 in a completely
different unit. That translation is fully contained inside
`AirConditionerAdapter`.

---

## Where to look if you want to extend this project

- **Add a new device type** (e.g. a smart lock): create
  `LegacyLock.java` in `legacy/`, create `LockAdapter.java` in
  `adapter/` that implements `SmartDevice`, then wrap and add it in
  `Main`. No changes needed to `SmartDevice` or
  `SmartHomeController`.
- **Change a conversion formula**: edit only the relevant adapter's
  `setLevel` method — the legacy class and the controller are
  unaffected.
- **Add a new controller operation** (e.g. `setLevelRange`): add it to
  `SmartHomeController` using only `SmartDevice` methods — never add a
  legacy-specific method there.
