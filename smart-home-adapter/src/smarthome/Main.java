package smarthome;

import smarthome.adapter.AirConditionerAdapter;
import smarthome.adapter.FanAdapter;
import smarthome.adapter.HeaterAdapter;
import smarthome.adapter.LightAdapter;
import smarthome.legacy.LegacyAirConditioner;
import smarthome.legacy.LegacyFan;
import smarthome.legacy.LegacyHeater;
import smarthome.legacy.LegacyLight;

/**
 * Demonstrates the Adapter Pattern: legacy devices with incompatible
 * interfaces are wrapped by adapters so the SmartHomeController can
 * operate them uniformly through the SmartDevice interface.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===== SMART HOME SYSTEM =====\n");

        // 1. Create the legacy devices (Adaptees).
        LegacyAirConditioner legacyAC = new LegacyAirConditioner();
        LegacyFan legacyFan = new LegacyFan();
        LegacyLight legacyLight = new LegacyLight();
        LegacyHeater legacyHeater = new LegacyHeater();

        // 2. Wrap each legacy device with its Adapter.
        SmartDevice ac = new AirConditionerAdapter(legacyAC);
        SmartDevice fan = new FanAdapter(legacyFan);
        SmartDevice light = new LightAdapter(legacyLight);
        SmartDevice heater = new HeaterAdapter(legacyHeater);

        // 3. Create the controller (Client).
        SmartHomeController controller = new SmartHomeController();

        // 4. Add all adapted devices to the controller.
        System.out.println("Adding devices...\n");
        controller.addDevice(ac);
        controller.addDevice(fan);
        controller.addDevice(light);
        controller.addDevice(heater);

        // 5. Turn all devices on.
        System.out.println("Turning on all devices...\n");
        controller.turnOnAll();

        // 6. Set all devices to a common level.
        System.out.println("\n===== SETTING LEVEL TO 5 =====\n");
        controller.setLevelAll(5);

        // 7. Display the status of all devices.
        System.out.println("\n===== DEVICE STATUS =====");
        controller.showAllStatus();

        // 8. Change individual device levels to show each Adapter's own conversion.
        System.out.println("\n===== INDIVIDUAL LEVEL CHANGES =====\n");
        ac.setLevel(1);
        ac.setLevel(10);
        fan.setLevel(8);
        light.setLevel(1);
        heater.setLevel(10);

        System.out.println("\n===== DEVICE STATUS AFTER CHANGES =====");
        controller.showAllStatus();

        // 9. Demonstrate validation of invalid levels.
        System.out.println("\n===== TESTING INVALID LEVELS =====\n");
        tryInvalidLevel(ac, 0);
        tryInvalidLevel(ac, 11);

        // 10. Turn all devices off.
        System.out.println("\n===== TURNING OFF =====\n");
        controller.turnOffAll();

        // 11. Display their final status.
        System.out.println("\n===== FINAL DEVICE STATUS =====");
        controller.showAllStatus();
    }

    private static void tryInvalidLevel(SmartDevice device, int level) {
        try {
            device.setLevel(level);
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected invalid level " + level + ": " + e.getMessage());
        }
    }
}
