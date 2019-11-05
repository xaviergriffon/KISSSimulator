package fr.griffon.enums;

public enum Band {
    A,
    B,
    E,
    FS,
    RB;
    public static String valueToString(int position) {
        if (position < 0 || position >= values().length) {
            System.out.println(position);
            return "bad value";
        }

        return values()[position].toString();
    }
}
