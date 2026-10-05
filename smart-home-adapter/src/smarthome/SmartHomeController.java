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
