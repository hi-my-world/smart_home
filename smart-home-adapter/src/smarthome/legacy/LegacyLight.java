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
