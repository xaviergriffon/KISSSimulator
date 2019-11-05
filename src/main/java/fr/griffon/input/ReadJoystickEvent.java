package fr.griffon.input;

import fr.griffon.Configuration;
import fr.griffon.ConfigurationManager;
import net.java.games.input.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capteur de joystick
 */
public class ReadJoystickEvent implements Runnable {

    static {
        System.setProperty("net.java.games.input.librarypath", System.getProperty("user.dir") + "/nativeLib");
    }

    private int yaw = 1500;
    private int pitch = 1500;
    private int throttle = 1500;
    private int roll = 1500;
    private boolean armed = false;
    private boolean run = false;


    public int getYaw() {
        return yaw;
    }

    public int getPitch() {
        return pitch;
    }

    public int getThrottle() {
        return throttle;
    }

    public int getRoll() {
        return roll;
    }

    public boolean getArmed() {
        return armed;
    }

    public void start() {
        run = true;
        Thread thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        run = false;
    }

    @Override
    public void run() {
        /* Get the available controllers */
        List<Controller> controllerToListen = new ArrayList<>();
        Controller[] controllers = ControllerEnvironment
                .getDefaultEnvironment().getControllers();
        if (controllers.length == 0) {
            System.out.println("Found no controllers.");
            return;
        }

        Configuration configuration = ConfigurationManager.getInstance().getConfiguration();
        List<InputSetup> inputSetups = configuration.getInputSetups();
        if (inputSetups == null || inputSetups.isEmpty()) {
            System.out.println("No input configured");
            // TODO : proposer une configuration
            return;
        }

        Map<Controller, InputSetup> controllerInputSetupMap = new HashMap<>();
        for (Controller controller :
                controllers) {
            System.out.println("Controller listened :");
            InputSetup inputSetup = configuration.getInputSetupForName(controller.getName());
            if (inputSetup != null) {
                controllerToListen.add(controller);
                controllerInputSetupMap.put(controller, inputSetup);
                System.out.println(controller.getName());
            }
        }

        if (controllerToListen.isEmpty()) {
            return;
        }
        while (run) {
            for (Controller controller : controllerToListen) {
                InputSetup inputSetup = controllerInputSetupMap.get(controller);
                controller.poll();
                EventQueue queue = controller.getEventQueue();
                Event event = new Event();
                while (queue.getNextEvent(event)) {
                    Component comp = event.getComponent();
                    String compName = comp.getIdentifier().getName();
                    float value = event.getValue();
                    if (comp.isAnalog()) {
                        // TODO : Contrôler si signe pour tous les axes
                        value = value * 500 * inputSetup.getYZSign() + 1500;
                        int roundValue = Math.round(value);
                        if (inputSetup.getYaw().equals(compName)) {
                            yaw = roundValue;
                        } else if (inputSetup.getRoll().equals(compName)) {
                            roll = roundValue;
                        } else if (inputSetup.getPitch().equals(compName)) {
                            pitch = roundValue;
                        } else if (inputSetup.getThrottle().equals(compName)) {
                            throttle = roundValue;
                        } else if (inputSetup.getArmed().equals(compName)) {
                            armed = roundValue > 1000;
                        } else {
                            System.out.println(compName);
                            System.out.println(roundValue);
                        }
                    } else {
                        if (inputSetup.getArmed().equals(compName)) {
                            if (value == 1) {
                                armed = !armed;
                            }
                        } else {
                            System.out.println(compName);
                            System.out.println(value);
                        }
                    }
                }
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
