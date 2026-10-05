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
