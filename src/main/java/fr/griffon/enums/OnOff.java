package fr.griffon.enums;

public enum OnOff {
    OFF,
    ON;

    public static String valueToString(int position) {
        if (position < 0 || position >= values().length) {
            System.out.println(position);
            return "bad value";
        }

        return values()[position].toString();
    }
}
