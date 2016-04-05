package main.java.elegit;

/**
 * Created by dmusican on 3/22/16.
 */
public enum AuthMethod {
    HTTP(0), HTTPS(1), SSHPASSWORD(2);
    private final int enumValue;

    AuthMethod(int enumValue) {
        this.enumValue = enumValue;
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
}
