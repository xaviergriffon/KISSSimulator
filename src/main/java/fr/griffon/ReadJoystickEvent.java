package fr.griffon;

import net.java.games.input.*;

import java.util.*;

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
            Controller controller = controllers[i];
            System.out.println(controller.getName());
            if (joystickController.getName().equals(controller.getName())) {
                controllerToListen.add(controller);
            }
        }
        while (run) {

            for (Controller controller : controllerToListen) {
                controller.poll();
                EventQueue queue = controller.getEventQueue();
                Event event = new Event();
                JoystickController joystickController = JoystickController.getByName(controller.getName());
                JoystickAxesNames axesNames = joystickController != null ? joystickController.getAxesNames() : null;
                while (axesNames != null && queue.getNextEvent(event)) {
                    Component comp = event.getComponent();
                    String compName = comp.getIdentifier().getName();
                    //System.out.println(compName);
                    float value = event.getValue();
                    boolean xboxController = JoystickController.XBOX360CONTROLLER.equals(joystickController) || JoystickController.XBOXWIRELESSCONTROLLER.equals(joystickController);
                    if (comp.isAnalog()) {
                        // TODO : déléguer le calcul à axesNames
                        if (JoystickController.TARANIS.equals(joystickController)) {
                            value = (value * 500 + 1500);
                        } else if (xboxController) {
                            value = (value * 500 * (axesNames.getPitch().equals(compName) || axesNames.getThrottle().equals(compName) ? -1 : 1) + 1500);
                        }
                        int roundValue = Math.round(value);
                        if (axesNames.getYaw().equals(compName)) {
                            yaw = roundValue;
                        } else if (axesNames.getRoll().equals(compName)) {
                            roll = roundValue;
                        } else if (axesNames.getPitch().equals(compName)) {
                            pitch = roundValue;
                        } else if (axesNames.getThrottle().equals(compName)) {
                            throttle = roundValue;
                        } else if (axesNames.getArmed().equals(compName)) {
                            armed = roundValue > 1000;
                        } else {
                            System.out.println(compName);
                            System.out.println(roundValue);
                        }
                    } else {
                        if (xboxController) {
                            if (axesNames.getArmed().equals(compName)) {
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
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO : A revoir pour faire une configuration dans un fichier

    enum JoystickController {
        XBOX360CONTROLLER(OsUtils.isWindows() ? "Controller (XBOX 360 For Windows)"  : "Xbox 360 Wired Controller"),
        TARANIS("FrSky Taranis Joystick"),
        XBOXWIRELESSCONTROLLER(OsUtils.isOSX() ? "Xbox Wireless Controller" : "vJoy Device");


        private static Map<String, JoystickController> controllerByName;

        private String name;
        private JoystickAxesNames axesNames;

        JoystickController(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public JoystickAxesNames getAxesNames() {
            if (axesNames == null) {
                this.axesNames = JoystickAxesNames.axesNamesForJoystick(this);
            }
            return axesNames;
        }

        public static JoystickController getByName(String name) {
            if (controllerByName == null){
                controllerByName = new HashMap<>();
                for (JoystickController joystickController : JoystickController.values()) {
                    controllerByName.put(joystickController.getName(), joystickController);
                }
            }
            return controllerByName.get(name);
        }

     }

     private static class JoystickAxesNames {
        private String yaw;
        private String roll;
        private String pitch;
        private String throttle;
        private String armed;

        public JoystickAxesNames(String yaw, String roll, String pitch,String throttle, String armed) {
            this.yaw = yaw;
            this.roll = roll;
            this.pitch = pitch;
            this.throttle = throttle;
            this.armed = armed;
        }

         public String getYaw() {
             return yaw;
         }

         public String getRoll() {
             return roll;
         }

         public String getPitch() {
             return pitch;
         }

         public String getThrottle() {
             return throttle;
         }

         public String getArmed() {
             return armed;
         }

         private static Map<JoystickController, JoystickAxesNames> joystickAxesNamesByControllers = new HashMap<>();

        public static JoystickAxesNames axesNamesForJoystick(JoystickController joystickController) {
            JoystickAxesNames axesNames = joystickAxesNamesByControllers.get(joystickController);
            if (axesNames == null) {
                switch (joystickController) {
                    case TARANIS:
                        axesNames = new JoystickAxesNames("x","rx",OsUtils.isOSX() ? "ry" : "y","z","ry");
                        break;
                    case XBOX360CONTROLLER:
                        axesNames = new JoystickAxesNames("x","rx","ry","y",OsUtils.isOSX() ? "8" :"7");
                        break;
                    case XBOXWIRELESSCONTROLLER:
                        axesNames = new JoystickAxesNames("x","z","rz","y","11");
                        break;
                }
                joystickAxesNamesByControllers.put(joystickController, axesNames);
            }
            return axesNames;
        }
     }
}
