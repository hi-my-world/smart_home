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
