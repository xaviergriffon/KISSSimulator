package fr.griffon;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.util.ArrayList;
import java.util.List;

public class Test {
    static {
        System.setProperty("net.java.games.input.librarypath", System.getProperty("user.dir") + "/nativeLib");
    }
    public static void main(String[] args) {

        Configuration configuration = ConfigurationManager.getInstance().getConfiguration();
        System.out.println(configuration.getPortName());

        List<Controller> controllerToListen = new ArrayList<>();
        Controller[] controllers = ControllerEnvironment
                .getDefaultEnvironment().getControllers();
        for (Controller controller : controllers) {
            System.out.println(controller.getName());
        }
    }
}
