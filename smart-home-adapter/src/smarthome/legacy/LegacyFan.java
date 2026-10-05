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
