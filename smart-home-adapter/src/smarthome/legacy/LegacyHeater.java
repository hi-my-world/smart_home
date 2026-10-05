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
