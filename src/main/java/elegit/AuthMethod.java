package main.java.elegit;

import java.util.ArrayList;

/**
 * Created by dmusican on 3/22/16.
 */
public enum AuthMethod {
    HTTP(0, "HTTP"),
    HTTPS(1, "HTTPS"),
    SSHPASSWORD(2, "SSH/Password"),
    SSHPUBLICKEY(3, "SSH/Public Key"),
    NONE(4, "NONE");

    private final int enumValue;
    private final String enumString;

    AuthMethod(int enumValue, String enumString) {
        this.enumValue = enumValue;
        this.enumString = enumString;
    }

    public int getEnumValue() {
        return enumValue;
    }

    public static AuthMethod getEnumFromValue(int value) {
        for (AuthMethod authMethod : AuthMethod.values()) {
            if (authMethod.enumValue == value)
                return authMethod;
        }
        throw new RuntimeException("Invalid value used to create AuthMethod.");
    }

    public static ArrayList<String> getStrings() {
        ArrayList<String> strings = new ArrayList<>();
        for (AuthMethod authMethod : AuthMethod.values()) {
            strings.add(authMethod.enumString);
        }
        return strings;
    }

    public static AuthMethod getEnumFromString(String string) {
        for (AuthMethod authMethod : AuthMethod.values()) {
            if (authMethod.enumString.equals(string))
                return authMethod;
        }
        throw new RuntimeException("Invalid string used to create AuthMethod.");
    }


}
