package fr.griffon.enums;

public enum VTXType {
    None("--"),
    Dummy("DUMMY VTX"),
    IRC("IRC TRAMP HV"),
    TBS_Unify("TBS UNIFY SMART AUDIO"),
    TBS_EVO("TBS EVO CROSSFIRE");

    private String description;
    private VTXType(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public static String valueToString(int position) {
        if (position < 0 || position >= values().length) {
            System.out.println(position);
            return "bad value";
        }

        return values()[position].getDescription();
    }
}
