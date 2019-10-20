package fr.griffon;

import net.java.games.input.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Capteur de joystick
 */
public class ReadJoystickEvent implements Runnable {

    private int yaw = 1500;
    private int pitch = 1500;
    private int throttle = 1500;
    private int roll = 1500;
    private boolean armed = false;
    private boolean run = true;
    private JoystickController joystickController;
    public ReadJoystickEvent(JoystickController joystickController) {
        Objects.requireNonNull(joystickController);
        this.joystickController = joystickController;
    }

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
        Thread thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        run = true;
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

        for (int i = 0; i < controllers.length; i++) {
            //System.out.println(controllers[i].getName());
            if (joystickController.getName().equals(controllers[i].getName())) {
                controllerToListen.add(controllers[i]);
            }
        }
        while (run) {

            for (Controller controller : controllerToListen) {
                controller.poll();
                EventQueue queue = controller.getEventQueue();
                Event event = new Event();
                while (queue.getNextEvent(event)) {
                    Component comp = event.getComponent();
                    String compName = comp.getName();
                    //System.out.println(compName);
                    float value = event.getValue();
                    if (comp.isAnalog()) {
                        if (JoystickController.TARANIS.getName().equals(controller.getName())) {
                            value = (value * 500 + 1500);
                            int roundValue = Math.round(value);
                            if ("x".equals(compName)) {
                                yaw = roundValue;
                            } else if ("rx".equals(compName)) {
                                roll = roundValue;
                            } else if ("y".equals(compName)) {
                                pitch = roundValue;
                            } else if ("z".equals(compName)) {
                                throttle = roundValue;
                            } else if ("ry".equals(compName)) {
                                armed = roundValue > 1000;
                            } else {
                                System.out.println(compName);
                                System.out.println(roundValue);
                            }
                        } else if (JoystickController.XBOX360CONTROLLER.getName().equals(controller.getName())) {
                            value = (value * 500 * (compName.endsWith("y") ? -1 : 1) + 1500);
                            int roundValue = Math.round(value);
                            if ("x".equals(compName)) {
                                yaw = roundValue;
                            } else if ("rx".equals(compName)) {
                                roll = roundValue;
                            } else if ("ry".equals(compName)) {
                                pitch = roundValue;
                            } else if ("y".equals(compName)) {
                                throttle = roundValue;
                            }
                        }
                    } else {
                        if (JoystickController.XBOX360CONTROLLER.getName().equals(controller.getName())) {
                            if ("8".equals(compName) && value == 1) {
                                armed = !armed;
                            }
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

    enum JoystickController {
        XBOX360CONTROLLER("Xbox 360 Wired Controller"),
        TARANIS("FrSky Taranis Joystick");

        private String name;

        JoystickController(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
