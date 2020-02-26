package com.absinthe.kage;

public class Settings {
    private static boolean deviceNecessary = true;

    public static boolean isDeviceNecessary() {
        return deviceNecessary;
    }

    public static void setDeviceNecessary(boolean deviceNecessary) {
        Settings.deviceNecessary = deviceNecessary;
    }
}
