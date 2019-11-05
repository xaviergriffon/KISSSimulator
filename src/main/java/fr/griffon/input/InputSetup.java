package fr.griffon.input;

public class InputSetup {

    private String name;
    private String type;
    private int YZSign = 1;
    private String yaw;
    private String roll;
    private String pitch;
    private String throttle;
    private String armed;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getYZSign() {
        return YZSign;
    }

    public void setYZSign(int YZSign) {
        this.YZSign = YZSign;
    }

    public String getYaw() {
        return yaw;
    }

    public void setYaw(String yaw) {
        this.yaw = yaw;
    }

    public String getRoll() {
        return roll;
    }

    public void setRoll(String roll) {
        this.roll = roll;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getThrottle() {
        return throttle;
    }

    public void setThrottle(String throttle) {
        this.throttle = throttle;
    }

    public String getArmed() {
        return armed;
    }

    public void setArmed(String armed) {
        this.armed = armed;
    }
}
