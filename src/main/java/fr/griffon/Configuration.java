package fr.griffon;

import fr.griffon.input.InputSetup;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {

    private String portName;
    private boolean logSetCommand;
    private boolean logGetCommand;
    private List<CommandResponse> commandResponses;
    private Map<String, CommandResponse> responseForCommand;
    private List<InputSetup> inputSetups;
    private Map<String,InputSetup> inputSetupForName;

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public boolean isLogSetCommand() {
        return logSetCommand;
    }

    public void setLogSetCommand(boolean logSetCommand) {
        this.logSetCommand = logSetCommand;
    }

    public boolean isLogGetCommand() {
        return logGetCommand;
    }

    public void setLogGetCommand(boolean logGetCommand) {
        this.logGetCommand = logGetCommand;
    }

    public List<CommandResponse> getCommandResponses() {
        return commandResponses;
    }

    public void setCommandResponses(List<CommandResponse> commandResponses) {
        this.commandResponses = Collections.unmodifiableList(commandResponses);
        this.responseForCommand = null;
    }

    public CommandResponse getResponseForCommand(String hexCommand) {
        if (responseForCommand == null) {
            responseForCommand = new HashMap<>();
            if (commandResponses != null) {
                commandResponses.forEach(commandResponse -> {
                    responseForCommand.put(commandResponse.getHexCommand(), commandResponse);
                });
            }
        }
        return responseForCommand.get(hexCommand);
    }

    public List<InputSetup> getInputSetups() {
        return inputSetups;
    }

    public void setInputSetups(List<InputSetup> inputSetups) {
        this.inputSetups = inputSetups;
        this.inputSetupForName = null;
    }

    public InputSetup getInputSetupForName(String inputName) {
        if (inputSetupForName == null) {
            inputSetupForName = new HashMap<>();
            if (inputSetups != null) {
                inputSetups.forEach(inputSetup -> {
                    inputSetupForName.put(inputSetup.getName(), inputSetup);
                });
            }
        }
        return inputSetupForName.get(inputName);
    }
}
