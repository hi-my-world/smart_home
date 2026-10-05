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
