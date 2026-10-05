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
