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
