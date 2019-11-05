package fr.griffon.enums;

public enum Filters {
    Off,
    High,
    MedHigh,
    Medium,
    MedLow,
    Low,
    VeryLow;

    public static String valueToString(int position) {
        if (position < 0 || position >= values().length) {
            System.out.println(position);
            return "bad value";
        }

        return values()[position].toString();
    }
}