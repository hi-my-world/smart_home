# Code Walkthrough — Smart Home Adapter Project

A **line-by-line** walkthrough of all 11 classes. Each section gives the
class's complete source, followed by a note on every line that does
something: what the Java means, why it is written that way, and what would
break if it were written differently.

| Role in the pattern | Classes | Section |
|---|---|---|
| Target (interface) | `SmartDevice` | [1](#1-the-target) |
| Adaptees (legacy) | `LegacyAirConditioner`, `LegacyFan`, `LegacyLight`, `LegacyHeater` | [2](#2-the-adaptees-the-legacy-package) |
| Adapters | `AirConditionerAdapter`, `FanAdapter`, `LightAdapter`, `HeaterAdapter` | [3](#3-the-adapters-the-adapter-package) |
| Client | `SmartHomeController` | [4](#4-the-client) |
| Demo / entry point | `Main` | [5](#5-the-demo) |

For the high-level pattern explanation and conversion tables, see
[README.md](README.md). To run the program, see
[README.md](README.md#how-to-compile-and-run).

**How to read this document.** Line numbers (`L12`) refer to the file shown
immediately above them, and they are generated from the real source, so they
stay accurate. If you only want the gist of a class, read its short
introduction and the source listing; the line notes are there for when you
want to know *why*.

---

## 1. The Target

### 1.1 `SmartDevice` — the Target interface

This is the **only** type the rest of the application depends on when it wants to control a device. Anything implementing these four methods can be plugged in.

**Full source** — `SmartDevice.java` (22 lines):

```java
package smarthome;

/**
 * Target interface of the Adapter Pattern.
 * The SmartHomeController (Client) depends only on this interface and
 * never on any concrete legacy device.
 */
public interface SmartDevice {

    void turnOn();

    void turnOff();

    /**
     * Sets the device's intensity using the application's unified scale.
     *
     * @param level a value from 1 (lowest) to 10 (highest)
     */
    void setLevel(int level);

    String getStatus();
}
```

**Line by line:**

- **L1** `package smarthome;`
  Declares the root package. Because of this line the file *must* live in a directory called `smarthome/`, and the class's full name is `smarthome.SmartDevice`.
- **L3–7** `/** …`
  Javadoc. It names the class's role in the pattern — useful, but a comment: it has no effect at runtime.
- **L8** `public interface SmartDevice {`
  `interface`, not `class`: it declares *what* a device can do and carries no fields, no constructor, and no method bodies. `public` matters — the adapters live in a different package (`smarthome.adapter`) and could not implement it otherwise.
- **L10** `void turnOn();`
  An abstract method: a signature ending in a semicolon instead of a body. In an interface it is implicitly `public abstract`, so neither keyword is written. `void` — turning a device on returns nothing.
- **L12** `void turnOff();`
  The mirror of `turnOn()`. Two separate methods rather than one `setPower(boolean)` because that is how every legacy device already models it.
- **L14–18** `/** …`
  This Javadoc block is where the **1–10 contract** is written down. Note the consequence: the range lives in a comment, so the compiler cannot enforce it. That is exactly why each adapter re-checks the range at runtime.
- **L19** `void setLevel(int level);`
  The heart of the pattern. It takes a plain `int` on the application's unified scale — a scale *no* legacy device actually speaks, which is what gives the adapters something to translate. The parameter name `level` is documentation only; callers pass by position.
- **L21** `String getStatus();`
  Returns a `String` rather than printing it, so the caller decides what to do with the text. This is the one method with a return value.

Four methods, no bodies, no state — the whole file is a promise. Every class in section 3 keeps that promise a different way.

---

## 2. The Adaptees (the `legacy/` package)

These four classes simulate "existing code you can't change" — think of them
as a third-party library you depend on but do not own.

They share a deliberate family resemblance: a `boolean` power flag, an
intensity field in the device's own unit, two power methods, a setter that
validates its own range and throws `IllegalArgumentException`, and one status
method. But **every method name differs**, and so does the *type* and *unit*
of intensity — Celsius, a word, a percentage, watts. That is the mismatch the
adapters absorb.

Two things to notice as you read:

- **None of them imports or implements `SmartDevice`.** Check the import
  lines: there are none at all. The one mention of `SmartDevice` anywhere in
  the package is inside a Javadoc comment, which the compiler does not treat
  as a dependency.
- **Every method prints a `[ClassName] …` line.** Those prints stand in for
  real hardware and, in the program's output, prove the legacy object really
  ran rather than the adapter faking it.

### 2.1 `LegacyAirConditioner`

Intensity as a **Celsius temperature**, 16–30, where *lower* means stronger cooling — an inverted scale the adapter has to flip.

**Full source** — `legacy/LegacyAirConditioner.java` (35 lines):

```java
package smarthome.legacy;

/**
 * Adaptee. Represents a pre-existing / third-party air conditioner API.
 * This class knows nothing about {@link smarthome.SmartDevice} and must
 * not be changed to support the new application.
 */
public class LegacyAirConditioner {

    private boolean cooling = false;
    private int temperature = 24; // degrees Celsius, valid range 16-30

    public void startCooling() {
        cooling = true;
        System.out.println("[LegacyAirConditioner] Cooling started.");
    }

    public void stopCooling() {
        cooling = false;
        System.out.println("[LegacyAirConditioner] Cooling stopped.");
    }

    public void setTemperature(int temperature) {
        if (temperature < 16 || temperature > 30) {
            throw new IllegalArgumentException("Temperature must be between 16 and 30.");
        }
        this.temperature = temperature;
        System.out.println("[LegacyAirConditioner] Temperature set to " + temperature + "C.");
    }

    public String getACState() {
        String power = cooling ? "ON" : "OFF";
        return "Air Conditioner: " + power + " | Temperature: " + temperature + "C";
    }
}
```

**Line by line:**

- **L1** `package smarthome.legacy;`
  A different package from `SmartDevice`. The package split is what makes the separation visible at a glance: nothing in `smarthome.legacy` imports anything from the application.
- **L3–7** `/** …`
  The Javadoc mentions `{@link smarthome.SmartDevice}`, but a Javadoc link is not a code dependency — delete the interface and this file still compiles.
- **L8** `public class LegacyAirConditioner {`
  A plain class with no `implements` clause. **This is the Adaptee**: it cannot be handed to the controller, and we are not allowed to add `implements SmartDevice` to fix that.
- **L10** `private boolean cooling = false;`
  The power flag, `private` so only this class can touch it. The `= false` is explicit even though `boolean` already defaults to `false` — it documents that the device starts switched off.
- **L11** `private int temperature = 24; // degrees Celsius, valid range 16-30`
  The intensity field, in *this device's* unit. It starts at 24 (mid-range), and the trailing comment is the only place the 16–30 contract is written down.
- **L13** `public void startCooling() {`
  Powers the device on — but it is called `startCooling`, not `turnOn`. This name mismatch alone is enough to make the class unusable through `SmartDevice`.
- **L14** `cooling = true;`
  Assigns the field directly. No `this.` prefix is needed because no parameter or local variable named `cooling` shadows it here.
- **L15** `System.out.println("[LegacyAirConditioner] Cooling started.");`
  Prints a `[ClassName]`-tagged line. These prints stand in for real hardware doing something, and in the program's output they prove the *legacy* object ran — not just the adapter.
- **L18–20** `public void stopCooling() { …`
  The exact mirror of `startCooling()`: sets the flag to `false` and prints its own tagged line. Two methods instead of one `setPower(boolean)` — a style every device in this project shares, and one reason `SmartDevice` also has two.
- **L23** `public void setTemperature(int temperature) {`
  The intensity setter, in degrees. The parameter is deliberately named the same as the field, which **shadows** it — inside this method, plain `temperature` means the parameter.
- **L24** `if (temperature < 16 || temperature > 30) {`
  A guard clause validating the device's *own* range. `temperature` here is the parameter (nearest scope wins), so this checks the incoming value.
- **L25** `throw new IllegalArgumentException("Temperature must be between 16 and 30.");`
  `IllegalArgumentException` is *unchecked*, so the method needs no `throws` clause and callers are not forced to catch it. Throwing exits the method immediately — the field below is never reached, so a rejected value leaves the device untouched.
- **L27** `this.temperature = temperature;`
  Here `this.` is **required**: without it this would assign the parameter to itself and silently do nothing. Reached only if the guard above passed.
- **L28** `System.out.println("[LegacyAirConditioner] Temperature set to " + temperature + "C.");`
  Confirms the accepted value. `+` with a `String` on the left converts the `int` to text automatically.
- **L31** `public String getACState() {`
  The status reader — named `getACState`, which no other device in the project matches (`checkFanStatus`, `lightStatus`, `readHeaterStatus`). Four names for one idea is the problem the adapters solve.
- **L32** `String power = cooling ? "ON" : "OFF";`
  The ternary conditional: reads *if `cooling` then `"ON"` else `"OFF"`*. A compact if/else that produces a value.
- **L33** `return "Air Conditioner: " + power + " | Temperature: " + temperature + "C";`
  Builds the display line by concatenation and returns it. The device decides its own formatting, which is why `getStatus()` on the adapter can pass it straight through.
### 2.2 `LegacyFan`

The hardest adaptee: its intensity is not a number at all but one of three **words**, so its adapter must convert numeric → categorical.

**Full source** — `legacy/LegacyFan.java` (33 lines):

```java
package smarthome.legacy;

/**
 * Adaptee. Represents a pre-existing fan API with its own speed vocabulary.
 */
public class LegacyFan {

    private boolean running = false;
    private String speed = "LOW"; // one of LOW, MEDIUM, HIGH

    public void startFan() {
        running = true;
        System.out.println("[LegacyFan] Fan started.");
    }

    public void stopFan() {
        running = false;
        System.out.println("[LegacyFan] Fan stopped.");
    }

    public void changeSpeed(String speed) {
        if (!speed.equals("LOW") && !speed.equals("MEDIUM") && !speed.equals("HIGH")) {
            throw new IllegalArgumentException("Speed must be LOW, MEDIUM, or HIGH.");
        }
        this.speed = speed;
        System.out.println("[LegacyFan] Speed changed to " + speed + ".");
    }

    public String checkFanStatus() {
        String power = running ? "ON" : "OFF";
        return "Fan: " + power + " | Speed: " + speed;
    }
}
```

**Line by line:**

- **L1** `package smarthome.legacy;`
  Same legacy package, same isolation from the application.
- **L3–5** `/** …`
  A one-line Javadoc; this class is simple enough not to need more.
- **L6** `public class LegacyFan {`
  Again no `implements`. Structurally a twin of the air conditioner — flag, intensity, two power methods, a setter, a status reader — but every name and the intensity *type* differ.
- **L8** `private boolean running = false;`
  The power flag, called `running` here rather than `cooling`. Same concept, third different name in the project.
- **L9** `private String speed = "LOW"; // one of LOW, MEDIUM, HIGH`
  Intensity as a **`String`**, defaulting to `"LOW"`. This is why the fan needs the most interesting adapter in the project: there is no arithmetic that turns 7 into a word.
- **L11–13** `public void startFan() { …`
  Powers on: sets the flag (no `this.` needed, nothing shadows it) and prints its tagged line. Structurally identical to `startCooling()` — only the names differ.
- **L16–18** `public void stopFan() { …`
  Powers off. Note `startFan`/`stopFan` versus the AC's `startCooling`/`stopCooling` — the adapters exist precisely to absorb this churn.
- **L21** `public void changeSpeed(String speed) {`
  The intensity setter, and not even called `set…`. Its parameter is a `String`, so the compiler will happily accept any text at all; correctness is enforced only by the check below. The parameter shadows the field.
- **L22** `if (!speed.equals("LOW") && !speed.equals("MEDIUM") && !speed.equals("HIGH")) {`
  Validates against three permitted strings. It uses `.equals()` — which compares *contents* — not `==`, which would compare object identity and fail unpredictably. The check is case-sensitive, so `"low"` is rejected; and because `speed.equals` is called on the parameter, passing `null` throws `NullPointerException` rather than the tidy `IllegalArgumentException` below.
- **L23** `throw new IllegalArgumentException("Speed must be LOW, MEDIUM, or HIGH.");`
  Rejects anything else, leaving the fan's stored speed unchanged.
- **L25** `this.speed = speed;`
  `this.` again required because of the shadowing parameter.
- **L26** `System.out.println("[LegacyFan] Speed changed to " + speed + ".");`
  Echoes the accepted word.
- **L29** `public String checkFanStatus() {`
  The status reader — a fourth naming style (`check…`).
- **L30** `String power = running ? "ON" : "OFF";`
  The same ternary idiom as the air conditioner, over a differently named flag.
- **L31** `return "Fan: " + power + " | Speed: " + speed;`
  Returns `"Fan: ON | Speed: MEDIUM"`. No unit suffix here, unlike the AC's `C` — because the value is already a word.
### 2.3 `LegacyLight`

Intensity as a **percentage, 0–100**. The closest match to the application's scale, and so the simplest conversion — but note the ranges still disagree at the bottom.

**Full source** — `legacy/LegacyLight.java` (34 lines):

```java
package smarthome.legacy;

/**
 * Adaptee. Represents a pre-existing smart light API that uses a
 * percentage-based brightness value.
 */
public class LegacyLight {

    private boolean enabled = false;
    private int brightnessPercent = 50; // 0-100

    public void enableLight() {
        enabled = true;
        System.out.println("[LegacyLight] Light enabled.");
    }

    public void disableLight() {
        enabled = false;
        System.out.println("[LegacyLight] Light disabled.");
    }

    public void setBrightness(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Brightness must be between 0 and 100.");
        }
        this.brightnessPercent = percentage;
        System.out.println("[LegacyLight] Brightness set to " + percentage + "%.");
    }

    public String lightStatus() {
        String power = enabled ? "ON" : "OFF";
        return "Light: " + power + " | Brightness: " + brightnessPercent + "%";
    }
}
```

**Line by line:**

- **L1** `package smarthome.legacy;`
  The legacy package again.
- **L3–6** `/** …`
  Javadoc noting the percentage-based brightness.
- **L7** `public class LegacyLight {`
  No `implements`; the same five-part shape as its siblings.
- **L9** `private boolean enabled = false;`
  The power flag, named `enabled` this time.
- **L10** `private int brightnessPercent = 50; // 0-100`
  Intensity as a percentage, starting at 50. **Its range starts at 0 while the application's scale starts at 1**, so the adapter's `level * 10` can never produce 0% — level 1 is 10%, and the bottom of the device's range is unreachable through `SmartDevice`.
- **L12–14** `public void enableLight() { …`
  Powers on — `enableLight`, a third verb for the same action, with the same flag-plus-print body as its siblings.
- **L17–19** `public void disableLight() { …`
  Powers off.
- **L22** `public void setBrightness(int percentage) {`
  The intensity setter. Its parameter is called `percentage`, **not** `brightnessPercent`, so unlike the AC and fan there is no shadowing here.
- **L23** `if (percentage < 0 || percentage > 100) {`
  Guard clause for this device's own 0–100 range.
- **L24** `throw new IllegalArgumentException("Brightness must be between 0 and 100.");`
  Same unchecked-exception strategy as the other adaptees.
- **L26** `this.brightnessPercent = percentage;`
  Here `this.` is **optional** — the names differ, so `brightnessPercent = percentage` would compile and behave identically. It is written anyway, for consistency with the other classes.
- **L27** `System.out.println("[LegacyLight] Brightness set to " + percentage + "%.");`
  Echoes the value with a `%` suffix.
- **L30** `public String lightStatus() {`
  The status reader, named with no verb at all — a fourth naming convention.
- **L31** `String power = enabled ? "ON" : "OFF";`
  The familiar ternary.
- **L32** `return "Light: " + power + " | Brightness: " + brightnessPercent + "%";`
  Returns `"Light: ON | Brightness: 50%"`.
### 2.4 `LegacyHeater`

Intensity as **wattage, 100–1000** — a unit with nothing in common with the other three, included to prove an adapter can convert anything.

**Full source** — `legacy/LegacyHeater.java` (35 lines):

```java
package smarthome.legacy;

/**
 * Adaptee. Represents a pre-existing heater API. Unlike the application's
 * 1-10 level scale, this legacy device expresses its heating intensity as
 * a wattage value between 100W and 1000W.
 */
public class LegacyHeater {

    private boolean heating = false;
    private int wattage = 100; // 100-1000, in steps of 100

    public void startHeating() {
        heating = true;
        System.out.println("[LegacyHeater] Heating started.");
    }

    public void stopHeating() {
        heating = false;
        System.out.println("[LegacyHeater] Heating stopped.");
    }

    public void setHeatLevel(int wattage) {
        if (wattage < 100 || wattage > 1000) {
            throw new IllegalArgumentException("Wattage must be between 100 and 1000.");
        }
        this.wattage = wattage;
        System.out.println("[LegacyHeater] Heat level set to " + wattage + "W.");
    }

    public String readHeaterStatus() {
        String power = heating ? "ON" : "OFF";
        return "Heater: " + power + " | Power: " + wattage + "W";
    }
}
```

**Line by line:**

- **L1** `package smarthome.legacy;`
  The legacy package, for the last time.
- **L3–7** `/** …`
  This Javadoc is the longest of the four because its unit needs the most explaining.
- **L8** `public class LegacyHeater {`
  No `implements`, same shape as the rest.
- **L10** `private boolean heating = false;`
  The power flag, named `heating`.
- **L11** `private int wattage = 100; // 100-1000, in steps of 100`
  Intensity in watts, starting at the minimum. The *step* of 100 is what makes `level * 100` land exactly on legal values — the 1–10 scale and this field happen to have the same number of positions.
- **L13–15** `public void startHeating() { …`
  Powers on — the fourth variation on the same three-line body.
- **L18–20** `public void stopHeating() { …`
  Powers off. Four classes, four pairs of power methods, eight distinct method names for two actions: the mismatch in miniature.
- **L23** `public void setHeatLevel(int wattage) {`
  A naming trap: the method is called `setHeat**Level**`, but it does **not** take a 1–10 level — it takes watts. An adapter author who trusted the name and forwarded `level` unchanged would set 5W and trigger the exception below. The parameter also shadows the field.
- **L24** `if (wattage < 100 || wattage > 1000) {`
  Guard clause over the watt range.
- **L25** `throw new IllegalArgumentException("Wattage must be between 100 and 1000.");`
  Rejects out-of-range wattage — including the raw 1–10 level, as above.
- **L27** `this.wattage = wattage;`
  `this.` required: the parameter shadows the field.
- **L28** `System.out.println("[LegacyHeater] Heat level set to " + wattage + "W.");`
  Echoes with a `W` suffix.
- **L31** `public String readHeaterStatus() {`
  The status reader — `read…`, the fourth and final naming style.
- **L32** `String power = heating ? "ON" : "OFF";`
  The ternary once more.
- **L33** `return "Heater: " + power + " | Power: " + wattage + "W";`
  Returns `"Heater: ON | Power: 500W"`.
---

## 3. The Adapters (the `adapter/` package)

Four small classes, each with the identical five-member shape:

1. a `private final` field holding one legacy object,
2. a constructor that receives it,
3. `turnOn()` and `turnOff()` — one-line forwarding,
4. `setLevel(int)` — **the only method that does real work**,
5. `getStatus()` — one-line forwarding, plus a `private validateLevel` helper.

So the four adapters differ in exactly two places: the type of the wrapped
field, and the body of `setLevel`. Everything else is the same code with
different nouns, and the line notes below point out which is which.

### 3.1 `AirConditionerAdapter`

The most mathematically interesting adapter: it maps 1–10 onto 16–30 °C **backwards**, because a higher level must mean colder air.

**Full source** — `adapter/AirConditionerAdapter.java` (56 lines):

```java
package smarthome.adapter;

import smarthome.SmartDevice;
import smarthome.legacy.LegacyAirConditioner;

/**
 * Adapter that lets a {@link LegacyAirConditioner} be controlled through
 * the unified {@link SmartDevice} interface.
 *
 * Level conversion: the application's 1-10 scale is mapped onto the
 * legacy 16C-30C range, where a higher level means stronger cooling
 * (lower temperature):
 *
 *   temperature = 30 - (level - 1) * (14 / 9)
 *
 * Level 1  -> 30C
 * Level 5  -> 24C
 * Level 10 -> 16C
 */
public class AirConditionerAdapter implements SmartDevice {

    private final LegacyAirConditioner ac;

    public AirConditionerAdapter(LegacyAirConditioner ac) {
        this.ac = ac;
    }

    @Override
    public void turnOn() {
        ac.startCooling();
    }

    @Override
    public void turnOff() {
        ac.stopCooling();
    }

    @Override
    public void setLevel(int level) {
        validateLevel(level);
        int temperature = (int) Math.round(30 - (level - 1) * (14.0 / 9.0));
        System.out.println("AC Adapter: Level " + level + " -> Temperature " + temperature + "C");
        ac.setTemperature(temperature);
    }

    @Override
    public String getStatus() {
        return ac.getACState();
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome.adapter;`
  A third package. The project's three packages map exactly onto three roles: `smarthome` (target + client), `smarthome.legacy` (adaptees), `smarthome.adapter` (adapters).
- **L3** `import smarthome.SmartDevice;`
  Needed because `SmartDevice` lives in a different package. Classes in the same package never need importing, which is why the legacy files have no imports at all.
- **L4** `import smarthome.legacy.LegacyAirConditioner;`
  **The important import.** An adapter is the only kind of class that imports from both worlds at once — the application's interface *and* one legacy device. That double knowledge is what lets it translate, and keeping it here is what keeps it out of the controller.
- **L6–19** `/** …`
  The Javadoc states the conversion formula and tabulates three sample levels — the kind of comment worth writing, because the formula below is not self-evident.
- **L20** `public class AirConditionerAdapter implements SmartDevice {`
  `implements SmartDevice` is the promise that makes this class usable by the controller. The compiler now **requires** all four interface methods; omitting one is a compile error, not a runtime surprise.
- **L22** `private final LegacyAirConditioner ac;`
  **Composition**: the adapter *has a* legacy device rather than *being* one. `private` hides it (nobody can reach past the adapter to the raw device), and `final` means it must be assigned exactly once, in the constructor, and can never be repointed afterwards. Note the declared type is the concrete legacy class — the adapter is the one place that specific type is still named.
- **L24** `public AirConditionerAdapter(LegacyAirConditioner ac) {`
  The constructor: same name as the class, no return type. It takes the device to wrap, so the adapter never creates its own — the caller decides which object to adapt. This is constructor injection.
- **L25** `this.ac = ac;`
  Stores the reference; `this.` is required because the parameter shadows the field. **There is no null check** — `new AirConditionerAdapter(null)` compiles and constructs happily, then throws `NullPointerException` on the first method call. Adding `Objects.requireNonNull(ac)` here would fail fast instead.
- **L28–29** `@Override …`
  `@Override` is an annotation, not a keyword, and it is optional — but it asks the compiler to verify that this method really does implement something from the interface. Misspell it `turnOnn()` and you get an error here rather than a mysterious "class is not abstract" message. The method must be `public`, because interface methods are public and an implementation may never narrow visibility.
- **L30** `ac.startCooling();`
  The translation, in one line: the `SmartDevice` word `turnOn` becomes the legacy word `startCooling`. The adapter adds no behaviour of its own — it only forwards.
- **L33–34** `@Override …`
  The same pattern for powering down.
- **L35** `ac.stopCooling();`
  `turnOff` → `stopCooling`.
- **L38–39** `@Override …`
  The only method that does real work. Everything above and below is forwarding.
- **L40** `validateLevel(level);`
  Checked **first**, before any arithmetic: an invalid level never reaches the formula or the device.
- **L41** `int temperature = (int) Math.round(30 - (level - 1) * (14.0 / 9.0));`
  The conversion, read inside-out: `(level - 1)` shifts 1–10 to 0–9; `14.0 / 9.0` is the span (30 − 16 = 14 degrees) divided by the 9 steps between the scale's ends, written with `.0` so Java does **floating-point** division (plain `14 / 9` would be integer division yielding 1, and every level would be wrong); subtracting from 30 counts *downward*, which is what inverts the scale; `Math.round` picks the nearest whole degree but returns a `long` for a `double` argument, so the explicit `(int)` cast is required to compile.
- **L42** `System.out.println("AC Adapter: Level " + level + " -> Temperature " + temperature + "C");`
  Prints the translation itself, so the program's output shows both sides of the conversion — this line, then the `[LegacyAirConditioner]` line from the device.
- **L43** `ac.setTemperature(temperature);`
  Hands over a value the formula guarantees is inside 16–30, so the legacy guard clause will never fire for a valid level.
- **L46–47** `@Override …`
  The fourth and last interface method.
- **L48** `return ac.getACState();`
  Pure pass-through: the legacy class already formats its own status text, so there is nothing to convert.
- **L51** `private void validateLevel(int level) {`
  A `private` helper — **not** part of `SmartDevice`, so it is invisible to anyone holding the adapter as a `SmartDevice`. It is an internal detail, not API.
- **L52** `if (level < 1 || level > 10) {`
  Enforces the *application's* scale, the one documented only in the interface's Javadoc.
- **L53** `throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);`
  Includes the offending value in the message, which is what makes `Main`'s output read `… got: 11`.
### 3.2 `FanAdapter`

The only adapter whose conversion is not arithmetic: it turns a number into one of three words, and necessarily loses information doing so.

**Full source** — `adapter/FanAdapter.java` (58 lines):

```java
package smarthome.adapter;

import smarthome.SmartDevice;
import smarthome.legacy.LegacyFan;

/**
 * Adapter that lets a {@link LegacyFan} be controlled through the unified
 * {@link SmartDevice} interface.
 *
 * Level conversion:
 *   Levels 1-3  -> LOW
 *   Levels 4-7  -> MEDIUM
 *   Levels 8-10 -> HIGH
 */
public class FanAdapter implements SmartDevice {

    private final LegacyFan fan;

    public FanAdapter(LegacyFan fan) {
        this.fan = fan;
    }

    @Override
    public void turnOn() {
        fan.startFan();
    }

    @Override
    public void turnOff() {
        fan.stopFan();
    }

    @Override
    public void setLevel(int level) {
        validateLevel(level);
        String speed;
        if (level <= 3) {
            speed = "LOW";
        } else if (level <= 7) {
            speed = "MEDIUM";
        } else {
            speed = "HIGH";
        }
        System.out.println("Fan Adapter: Level " + level + " -> " + speed);
        fan.changeSpeed(speed);
    }

    @Override
    public String getStatus() {
        return fan.checkFanStatus();
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome.adapter;`
  The adapter package.
- **L3** `import smarthome.SmartDevice;`
  The target interface.
- **L4** `import smarthome.legacy.LegacyFan;`
  The one legacy class this adapter knows about — and the only one.
- **L6–14** `/** …`
  The Javadoc spells out the three level bands, which is the clearest form this conversion can take.
- **L15** `public class FanAdapter implements SmartDevice {`
  The same promise to the compiler as every other adapter.
- **L17** `private final LegacyFan fan;`
  Composition again, typed to `LegacyFan`. The four adapters differ in exactly this field's type and in `setLevel`.
- **L19** `public FanAdapter(LegacyFan fan) {`
  Constructor injection of the fan to wrap.
- **L20** `this.fan = fan;`
  `this.` required by shadowing; again no null check.
- **L23–24** `@Override …`
  Forwarding method.
- **L25** `fan.startFan();`
  `turnOn` → `startFan`. Compare `ac.startCooling()` — same interface method, different legacy vocabulary, and the caller sees neither.
- **L28–29** `@Override …`
  Forwarding method.
- **L30** `fan.stopFan();`
  `turnOff` → `stopFan`.
- **L33–34** `@Override …`
  The interesting one.
- **L35** `validateLevel(level);`
  The app-scale check runs first, as in every adapter — which is what lets the branching below skip a lower bound.
- **L36** `String speed;`
  Declared **without** an initial value. Java's "definite assignment" analysis allows this only because the if/else chain below assigns it on every possible path; drop the final `else` and the code stops compiling.
- **L37** `if (level <= 3) {`
  Levels 1–3. There is no `level >= 1` test because `validateLevel` already guaranteed it.
- **L38** `speed = "LOW";`
  The first of the three legal words.
- **L39** `} else if (level <= 7) {`
  Levels 4–7, reached only when `level > 3`.
- **L40** `speed = "MEDIUM";`
  The middle band — four levels wide, versus three for the others.
- **L41** `} else {`
  Levels 8–10. A bare `else` rather than `level <= 10`, which both satisfies the compiler's definite-assignment rule and relies on the validation above.
- **L42** `speed = "HIGH";`
  The top band.
- **L44** `System.out.println("Fan Adapter: Level " + level + " -> " + speed);`
  Shows the numeric → categorical jump in the output.
- **L45** `fan.changeSpeed(speed);`
  Passes one of exactly three strings the legacy class accepts, so its `.equals()` gauntlet always passes. **This conversion is lossy**: levels 4 and 7 both become `"MEDIUM"` and are indistinguishable afterwards — not a bug, but a real consequence of hardware with only three settings.
- **L48–49** `@Override …`
  The status method.
- **L50** `return fan.checkFanStatus();`
  Pass-through to the legacy name.
- **L53** `private void validateLevel(int level) {`
  **Identical** to the helper in the other three adapters — the same seven lines copied four times. See §6 on why that duplication is worth noticing.
- **L54** `if (level < 1 || level > 10) {`
  The app-scale bounds.
- **L55** `throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);`
  The same message as its three siblings.
### 3.3 `LightAdapter`

The simplest adapter in the project — one multiplication, no cast, no branching. Worth reading precisely because it shows how little an adapter can contain.

**Full source** — `adapter/LightAdapter.java` (55 lines):

```java
package smarthome.adapter;

import smarthome.SmartDevice;
import smarthome.legacy.LegacyLight;

/**
 * Adapter that lets a {@link LegacyLight} be controlled through the unified
 * {@link SmartDevice} interface.
 *
 * Level conversion: the 1-10 scale is mapped directly onto a 0-100%
 * brightness scale:
 *
 *   brightness = level * 10
 *
 * Level 1  -> 10%
 * Level 5  -> 50%
 * Level 10 -> 100%
 */
public class LightAdapter implements SmartDevice {

    private final LegacyLight light;

    public LightAdapter(LegacyLight light) {
        this.light = light;
    }

    @Override
    public void turnOn() {
        light.enableLight();
    }

    @Override
    public void turnOff() {
        light.disableLight();
    }

    @Override
    public void setLevel(int level) {
        validateLevel(level);
        int brightness = level * 10;
        System.out.println("Light Adapter: Level " + level + " -> " + brightness + "%");
        light.setBrightness(brightness);
    }

    @Override
    public String getStatus() {
        return light.lightStatus();
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome.adapter;`
  The adapter package.
- **L3** `import smarthome.SmartDevice;`
  The target interface.
- **L4** `import smarthome.legacy.LegacyLight;`
  Its one adaptee.
- **L6–18** `/** …`
  Javadoc with the formula and three worked examples.
- **L19** `public class LightAdapter implements SmartDevice {`
  The usual promise.
- **L21** `private final LegacyLight light;`
  Composition, typed to `LegacyLight`.
- **L23** `public LightAdapter(LegacyLight light) {`
  Constructor injection.
- **L24** `this.light = light;`
  `this.` required by shadowing; no null check.
- **L27–28** `@Override …`
  Forwarding method.
- **L29** `light.enableLight();`
  `turnOn` → `enableLight` — a third distinct verb for the same concept.
- **L32–33** `@Override …`
  Forwarding method.
- **L34** `light.disableLight();`
  `turnOff` → `disableLight`.
- **L37–38** `@Override …`
  The conversion method.
- **L39** `validateLevel(level);`
  The app-scale check first, as always.
- **L40** `int brightness = level * 10;`
  The entire conversion. Both operands are `int`, so this is integer arithmetic — no `double`, no `Math.round`, no cast, unlike the air conditioner. 1 → 10, 5 → 50, 10 → 100. Because the scale starts at 1, the device's legal 0% is unreachable from `SmartDevice`.
- **L41** `System.out.println("Light Adapter: Level " + level + " -> " + brightness + "%");`
  Reports the conversion with a `%` suffix.
- **L42** `light.setBrightness(brightness);`
  Always within the legacy 0–100 range for a valid level.
- **L45–46** `@Override …`
  The status method.
- **L47** `return light.lightStatus();`
  Pass-through.
- **L50** `private void validateLevel(int level) {`
  The fourth identical copy of the helper.
- **L51** `if (level < 1 || level > 10) {`
  The app-scale bounds.
- **L52** `throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);`
  Identical message again.
### 3.4 `HeaterAdapter`

Converts into the most alien unit — watts — yet stays as simple as the light, because the two scales happen to line up.

**Full source** — `adapter/HeaterAdapter.java` (55 lines):

```java
package smarthome.adapter;

import smarthome.SmartDevice;
import smarthome.legacy.LegacyHeater;

/**
 * Adapter that lets a {@link LegacyHeater} be controlled through the
 * unified {@link SmartDevice} interface.
 *
 * Level conversion: the legacy heater expects a wattage between 100W and
 * 1000W, so the 1-10 scale is mapped linearly:
 *
 *   wattage = level * 100
 *
 * Level 1  -> 100W
 * Level 5  -> 500W
 * Level 10 -> 1000W
 */
public class HeaterAdapter implements SmartDevice {

    private final LegacyHeater heater;

    public HeaterAdapter(LegacyHeater heater) {
        this.heater = heater;
    }

    @Override
    public void turnOn() {
        heater.startHeating();
    }

    @Override
    public void turnOff() {
        heater.stopHeating();
    }

    @Override
    public void setLevel(int level) {
        validateLevel(level);
        int wattage = level * 100;
        System.out.println("Heater Adapter: Level " + level + " -> " + wattage + "W");
        heater.setHeatLevel(wattage);
    }

    @Override
    public String getStatus() {
        return heater.readHeaterStatus();
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome.adapter;`
  The adapter package.
- **L3** `import smarthome.SmartDevice;`
  The target interface.
- **L4** `import smarthome.legacy.LegacyHeater;`
  Its one adaptee.
- **L6–18** `/** …`
  Javadoc explaining the watt mapping.
- **L19** `public class HeaterAdapter implements SmartDevice {`
  The usual promise.
- **L21** `private final LegacyHeater heater;`
  Composition, typed to `LegacyHeater`.
- **L23** `public HeaterAdapter(LegacyHeater heater) {`
  Constructor injection.
- **L24** `this.heater = heater;`
  `this.` required by shadowing; no null check.
- **L27–28** `@Override …`
  Forwarding method.
- **L29** `heater.startHeating();`
  `turnOn` → `startHeating` — the fourth distinct verb.
- **L32–33** `@Override …`
  Forwarding method.
- **L34** `heater.stopHeating();`
  `turnOff` → `stopHeating`.
- **L37–38** `@Override …`
  The conversion method.
- **L39** `validateLevel(level);`
  App-scale check first.
- **L40** `int wattage = level * 100;`
  Integer arithmetic again: 1 → 100W, 5 → 500W, 10 → 1000W. This lands exactly on the device's 100–1000 range, start and end, because the legacy field moves in steps of 100 and the app scale has ten positions. Nothing is lost here — unlike the fan.
- **L41** `System.out.println("Heater Adapter: Level " + level + " -> " + wattage + "W");`
  Reports the conversion with a `W` suffix.
- **L42** `heater.setHeatLevel(wattage);`
  Note what is passed: `wattage`, **not** `level`. The legacy method's name says "Level" but its contract is watts, and this line is where that trap is avoided.
- **L45–46** `@Override …`
  The status method.
- **L47** `return heater.readHeaterStatus();`
  Pass-through.
- **L50** `private void validateLevel(int level) {`
  The last identical copy.
- **L51** `if (level < 1 || level > 10) {`
  The app-scale bounds.
- **L52** `throw new IllegalArgumentException("Level must be between 1 and 10, got: " + level);`
  Identical message.
---

## 4. The Client

### 4.1 `SmartHomeController` — the Client

The payoff. Read this file looking for what is **absent**: no legacy import, no `instanceof`, no device-specific branch anywhere.

**Full source** — `SmartHomeController.java` (46 lines):

```java
package smarthome;

import java.util.ArrayList;
import java.util.List;

/**
 * Client of the Adapter Pattern. The controller only knows about the
 * {@link SmartDevice} target interface and has no knowledge of any
 * specific legacy device or adapter implementation.
 */
public class SmartHomeController {

    private final List<SmartDevice> devices = new ArrayList<>();

    public void addDevice(SmartDevice device) {
        devices.add(device);
    }

    public void removeDevice(SmartDevice device) {
        devices.remove(device);
    }

    public void turnOnAll() {
        for (SmartDevice device : devices) {
            device.turnOn();
        }
    }

    public void turnOffAll() {
        for (SmartDevice device : devices) {
            device.turnOff();
        }
    }

    public void setLevelAll(int level) {
        for (SmartDevice device : devices) {
            device.setLevel(level);
        }
    }

    public void showAllStatus() {
        for (SmartDevice device : devices) {
            System.out.println(device.getStatus());
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome;`
  The same package as `SmartDevice`, so no import is needed to use it.
- **L3** `import java.util.ArrayList;`
  The concrete list implementation.
- **L4** `import java.util.List;`
  The list *interface*. Two imports from the JDK — **and that is the complete import list**. There is no `import smarthome.legacy.*` and no `import smarthome.adapter.*`, which is the mechanical proof that the controller is decoupled from both.
- **L6–10** `/** …`
  Javadoc naming the role.
- **L11** `public class SmartHomeController {`
  An ordinary class — the client implements nothing and extends nothing.
- **L13** `private final List<SmartDevice> devices = new ArrayList<>();`
  A dense line worth unpacking. The field is declared as `List` (the interface) but assigned an `ArrayList` (the implementation) — the same "depend on the abstraction" instinct the whole project is about. `<SmartDevice>` is the generic type parameter: the list accepts **any** `SmartDevice`, which is precisely why all four adapters can sit in one collection. `<>` is the diamond operator, inferring the type from the left-hand side. `final` freezes the *reference*, not the contents: you can add and remove devices, but the field can never point at a different list.
- **L15** `public void addDevice(SmartDevice device) {`
  **The single most important signature in the project.** The parameter type is the interface, so an `AirConditionerAdapter`, a `FanAdapter`, or a future `SmartLockAdapter` are all equally acceptable and the method never changes.
- **L16** `devices.add(device);`
  Appends to the list. Duplicates are permitted — nothing stops the same device being added twice, which would make `turnOnAll()` call it twice.
- **L19** `public void removeDevice(SmartDevice device) {`
  Takes the interface type, symmetric with `addDevice`.
- **L20** `devices.remove(device);`
  `List.remove` finds the element using `equals()`. None of the adapters override `equals`, so the inherited `Object.equals` applies, which compares **references**. In practice: you must pass the very same adapter object you added — an equivalent adapter wrapping the same legacy device will not match.
- **L23–24** `public void turnOnAll() { …`
  The first bulk operation, and the first of four identical enhanced `for` loops. The loop variable's **static type is `SmartDevice`** — at compile time that is everything the controller knows about the object it is holding.
- **L25** `device.turnOn();`
  **Dynamic dispatch, the mechanism the whole pattern rests on.** The compiler only verifies that `SmartDevice` declares `turnOn()`; the JVM looks at the object's *actual* runtime class and runs that class's version. So this one line of source executes four different method bodies in four iterations — `AirConditionerAdapter.turnOn()`, then `FanAdapter.turnOn()`, and so on. That is what replaces the chain of `instanceof` checks this file would otherwise need.
- **L29–30** `public void turnOffAll() { …`
  The mirror of `turnOnAll`, structurally identical down to the loop.
- **L31** `device.turnOff();`
  The same dispatch for powering down.
- **L35–36** `public void setLevelAll(int level) { …`
  Takes a level on the application's unified scale and applies it to everything, over the same loop.
- **L37** `device.setLevel(level);`
  One number, four unit systems — the controller hands over 5 and never learns that it became 24 °C, `"MEDIUM"`, 50% and 500W. Worth noting: nothing catches `IllegalArgumentException` here, so an invalid level aborts the loop partway and leaves the remaining devices untouched.
- **L41–42** `public void showAllStatus() { …`
  The reporting operation, over the fourth and last copy of the loop.
- **L43** `System.out.println(device.getStatus());`
  The controller decides to print; each device decided the wording. The only place this class produces output.

Six short methods, each a loop over an interface. Adding a fifth device type requires no edit to this file at all.

---

## 5. The Demo

### 5.1 `Main` — the demo and entry point

The wiring. This is the **only** file that names legacy classes, adapters and the controller together, which is exactly where that knowledge belongs.

**Full source** — `Main.java` (88 lines):

```java
package smarthome;

import smarthome.adapter.AirConditionerAdapter;
import smarthome.adapter.FanAdapter;
import smarthome.adapter.HeaterAdapter;
import smarthome.adapter.LightAdapter;
import smarthome.legacy.LegacyAirConditioner;
import smarthome.legacy.LegacyFan;
import smarthome.legacy.LegacyHeater;
import smarthome.legacy.LegacyLight;

/**
 * Demonstrates the Adapter Pattern: legacy devices with incompatible
 * interfaces are wrapped by adapters so the SmartHomeController can
 * operate them uniformly through the SmartDevice interface.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===== SMART HOME SYSTEM =====\n");

        // 1. Create the legacy devices (Adaptees).
        LegacyAirConditioner legacyAC = new LegacyAirConditioner();
        LegacyFan legacyFan = new LegacyFan();
        LegacyLight legacyLight = new LegacyLight();
        LegacyHeater legacyHeater = new LegacyHeater();

        // 2. Wrap each legacy device with its Adapter.
        SmartDevice ac = new AirConditionerAdapter(legacyAC);
        SmartDevice fan = new FanAdapter(legacyFan);
        SmartDevice light = new LightAdapter(legacyLight);
        SmartDevice heater = new HeaterAdapter(legacyHeater);

        // 3. Create the controller (Client).
        SmartHomeController controller = new SmartHomeController();

        // 4. Add all adapted devices to the controller.
        System.out.println("Adding devices...\n");
        controller.addDevice(ac);
        controller.addDevice(fan);
        controller.addDevice(light);
        controller.addDevice(heater);

        // 5. Turn all devices on.
        System.out.println("Turning on all devices...\n");
        controller.turnOnAll();

        // 6. Set all devices to a common level.
        System.out.println("\n===== SETTING LEVEL TO 5 =====\n");
        controller.setLevelAll(5);

        // 7. Display the status of all devices.
        System.out.println("\n===== DEVICE STATUS =====");
        controller.showAllStatus();

        // 8. Change individual device levels to show each Adapter's own conversion.
        System.out.println("\n===== INDIVIDUAL LEVEL CHANGES =====\n");
        ac.setLevel(1);
        ac.setLevel(10);
        fan.setLevel(8);
        light.setLevel(1);
        heater.setLevel(10);

        System.out.println("\n===== DEVICE STATUS AFTER CHANGES =====");
        controller.showAllStatus();

        // 9. Demonstrate validation of invalid levels.
        System.out.println("\n===== TESTING INVALID LEVELS =====\n");
        tryInvalidLevel(ac, 0);
        tryInvalidLevel(ac, 11);

        // 10. Turn all devices off.
        System.out.println("\n===== TURNING OFF =====\n");
        controller.turnOffAll();

        // 11. Display their final status.
        System.out.println("\n===== FINAL DEVICE STATUS =====");
        controller.showAllStatus();
    }

    private static void tryInvalidLevel(SmartDevice device, int level) {
        try {
            device.setLevel(level);
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected invalid level " + level + ": " + e.getMessage());
        }
    }
}
```

**Line by line:**

- **L1** `package smarthome;`
  The root package, alongside `SmartDevice` and the controller.
- **L3–6** `import smarthome.adapter.AirConditionerAdapter; …`
  All four adapters, imported because they live in `smarthome.adapter`. `Main` needs their concrete names for one reason only: you cannot call `new` on an interface.
- **L7–10** `import smarthome.legacy.LegacyAirConditioner; …`
  And all four legacy devices. Eight imports in total: `Main` is where every concrete type in the project meets, and the only file that imports from both `adapter` and `legacy`. Wiring code is *supposed* to know everything; the point of the pattern is that nothing else does.
- **L12–16** `/** …`
  Javadoc describing the demo's purpose.
- **L17** `public class Main {`
  A class purely as a container for `main` — Java has no free-standing functions.
- **L19** `public static void main(String[] args) {`
  The JVM entry point, whose signature is fixed: `public` so the launcher can call it, `static` so it runs without constructing a `Main`, `void`, and a `String[]` of command-line arguments. `args` is never used here.
- **L20** `System.out.println("===== SMART HOME SYSTEM =====\n");`
  The banner. `\n` adds a blank line on top of `println`'s own line break.
- **L22** `// 1. Create the legacy devices (Adaptees).`
  The source's own numbered commentary; the eleven steps below follow it.
- **L23** `LegacyAirConditioner legacyAC = new LegacyAirConditioner();`
  **Step 1.** Concrete type on both sides of the `=`. You cannot dodge the specific type when *constructing* something — only when using it. These four objects stand in for pre-existing hardware.
- **L24** `LegacyFan legacyFan = new LegacyFan();`
  The fan, created with its own concrete type.
- **L25** `LegacyLight legacyLight = new LegacyLight();`
  The light.
- **L26** `LegacyHeater legacyHeater = new LegacyHeater();`
  The heater. At this instant none of the four can be given to the controller.
- **L29** `SmartDevice ac = new AirConditionerAdapter(legacyAC);`
  **Step 2, and the pivotal line of the program.** The right-hand side builds an `AirConditionerAdapter`; the left-hand side declares the variable as `SmartDevice`. That widening is an *upcast*, and it is deliberate: from here on the compiler will only let you call the four interface methods on `ac`, so the legacy device is sealed behind the adapter. The object is still an `AirConditionerAdapter` at runtime — which is what dynamic dispatch depends on.
- **L30** `SmartDevice fan = new FanAdapter(legacyFan);`
  The same wrap-and-upcast for the fan.
- **L31** `SmartDevice light = new LightAdapter(legacyLight);`
  And the light.
- **L32** `SmartDevice heater = new HeaterAdapter(legacyHeater);`
  And the heater. Four incompatible APIs are now four interchangeable `SmartDevice`s.
- **L35** `SmartHomeController controller = new SmartHomeController();`
  **Step 3.** The client, constructed with no arguments — it starts with an empty device list.
- **L38** `System.out.println("Adding devices...\n");`
  A step heading. The demo narrates itself so the output below is readable.
- **L39** `controller.addDevice(ac);`
  **Step 4.** Registers the air conditioner. The argument's static type is already `SmartDevice`, so this is an exact match for the parameter.
- **L40** `controller.addDevice(fan);`
  Registers the fan — the identical call, a different device.
- **L41** `controller.addDevice(light);`
  Registers the light.
- **L42** `controller.addDevice(heater);`
  Registers the heater. The controller now holds four devices it cannot tell apart.
- **L45–46** `System.out.println("Turning on all devices...\n"); …`
  **Step 5.** One call powers on four different kinds of hardware; the output's four `[Legacy…]` lines come from this.
- **L49–50** `System.out.println("\n===== SETTING LEVEL TO 5 =====\n"); …`
  **Step 6.** The pattern's clearest demonstration: a single `5` becomes 24 °C, `"MEDIUM"`, 50% and 500W, each conversion printed by its own adapter.
- **L53–54** `System.out.println("\n===== DEVICE STATUS ====="); …`
  **Step 7.** Prints four status strings, each formatted by its own legacy class.
- **L57** `System.out.println("\n===== INDIVIDUAL LEVEL CHANGES =====\n");`
  The step-8 heading.
- **L58** `ac.setLevel(1);`
  **Step 8** begins: the demo now addresses devices individually to show each adapter's conversion at the extremes. Level 1 → the warmest 30 °C. Note it calls `ac`, the `SmartDevice` variable — not the adapter type, and not the legacy object.
- **L59** `ac.setLevel(10);`
  Level 10 → the coldest 16 °C, proving the inverted mapping.
- **L60** `fan.setLevel(8);`
  Level 8 → the bottom of the `"HIGH"` band.
- **L61** `light.setLevel(1);`
  Level 1 → 10%, the lowest brightness this scale can express.
- **L62** `heater.setLevel(10);`
  Level 10 → 1000W, the top of the watt range.
- **L64–65** `System.out.println("\n===== DEVICE STATUS AFTER CHANGES ====="); …`
  Re-reads every status so the individual changes above are visible side by side.
- **L68** `System.out.println("\n===== TESTING INVALID LEVELS =====\n");`
  The step-9 heading.
- **L69** `tryInvalidLevel(ac, 0);`
  **Step 9.** 0 is below the application's scale. Routed through the helper so the exception is caught rather than killing the program.
- **L70** `tryInvalidLevel(ac, 11);`
  11 is above it. Both values are rejected by `validateLevel` before any conversion math runs.
- **L73–74** `System.out.println("\n===== TURNING OFF =====\n"); …`
  **Step 10.** Powers everything down through the interface. That this line runs at all is the point of the `try`/`catch` below — the two rejected levels did not stop the program.
- **L77–78** `System.out.println("\n===== FINAL DEVICE STATUS ====="); …`
  **Step 11.** The closing report. Each device shows `OFF` but **keeps** the level it was last set to — powering down does not reset intensity, because no legacy class touches its intensity field in `stop…`/`disable…`.
- **L81** `private static void tryInvalidLevel(SmartDevice device, int level) {`
  **Step 11's helper.** `private` (used only here) and `static` (callable from the static `main` without an instance). Note the parameter type: even the error-handling demo talks to a `SmartDevice`, not to a specific adapter.
- **L82** `try {`
  Opens the protected block.
- **L83** `device.setLevel(level);`
  The call expected to fail. The exception is thrown deep inside `AirConditionerAdapter.validateLevel` and propagates up to here.
- **L84** `} catch (IllegalArgumentException e) {`
  Catches exactly the exception type the adapters throw — not a blanket `catch (Exception e)`, which would also swallow genuine bugs.
- **L85** `System.out.println("Rejected invalid level " + level + ": " + e.getMessage());`
  `e.getMessage()` retrieves the text built in the adapter, which is why the output ends with `got: 11`. Because the exception is caught and not rethrown, the demo continues to step 10 — demonstrating that bad input is rejected *without* the device or the program being left broken.

---

## 6. Observations across the whole codebase

Things that only become visible once you have read every line:

- **`validateLevel` is duplicated four times, verbatim.** All four adapters
  carry the same seven-line helper with the same message. It works, and it
  keeps each adapter self-contained, but a shared `abstract` base class (or a
  small static utility) would hold the application's 1–10 contract in one
  place. As written, changing the scale means editing four files and risking
  drift between them.
- **No adapter checks its constructor argument for `null`.**
  `new FanAdapter(null)` constructs successfully and then throws
  `NullPointerException` on the first call, far from the real mistake.
  `Objects.requireNonNull(fan)` in each constructor would fail at the point
  of error instead.
- **Validation happens twice, by design.** The adapter checks 1–10 (the
  application's contract) and the legacy class independently checks its own
  range — 16–30, three words, 0–100, 100–1000. Neither trusts the other, and
  neither has to know the other's rules.
- **Two conversions are leaky at the edges.** The fan's is lossy (levels 4
  and 7 are both `"MEDIUM"`), and the light's cannot reach the device's legal
  0% because the application scale starts at 1. Both are honest consequences
  of mapping scales that do not line up.
- **`setLevelAll` has no error handling.** One device throwing
  `IllegalArgumentException` aborts the loop, so devices earlier in the list
  are changed and later ones are not. Fine for a demo; a real system would
  decide deliberately between failing fast and continuing.
- **The decoupling is checkable, not just claimed.** `SmartHomeController`
  imports only `java.util.List` and `java.util.ArrayList`; it contains no
  `instanceof` and no legacy type. That is the property the pattern buys, and
  it is visible in the import block.

---

## How a single call flows through the system

`controller.setLevelAll(5)`, followed through for the air conditioner:

```
Main
  -> controller.setLevelAll(5)
       -> for (SmartDevice device : devices)        // static type: SmartDevice
            -> device.setLevel(5)                   // dynamic dispatch...
                 -> AirConditionerAdapter.setLevel(5)   // ...lands here at runtime
                      - validateLevel(5)                // 1 <= 5 <= 10, passes
                      - (level - 1)        = 4
                      - 14.0 / 9.0         = 1.555...   // double division
                      - 30 - 4 * 1.555...  = 23.777...
                      - Math.round(...)    = 24         // long, cast to int
                      - println("AC Adapter: Level 5 -> Temperature 24C")
                      -> legacyAC.setTemperature(24)
                           - 16 <= 24 <= 30, passes
                           - this.temperature = 24
                           - println("[LegacyAirConditioner] Temperature set to 24C.")
```

The controller wrote one line, `device.setLevel(level)`. It never learns that
5 became 24, nor that the same line simultaneously produced `"MEDIUM"`, 50%
and 500W for the other three devices. Every bit of that translation lives in
the adapters.

---

## Where to look if you want to extend this project

- **Add a new device type** (e.g. a smart lock): create `LegacyLock.java` in
  `legacy/`, create `LockAdapter.java` in `adapter/` implementing
  `SmartDevice`, then wrap and register it in `Main`. `SmartDevice` and
  `SmartHomeController` are not touched.
- **Change a conversion formula**: edit one adapter's `setLevel`. The legacy
  class and the controller are unaffected.
- **Change the 1–10 scale**: this is the one change that is *not* localised —
  see the duplication note in §6.
- **Add a controller operation** (e.g. `setLevelRange`): add it to
  `SmartHomeController` using only `SmartDevice` methods. Never add a
  legacy-specific method there; that would undo the pattern.
