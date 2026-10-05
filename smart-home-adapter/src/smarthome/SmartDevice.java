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
