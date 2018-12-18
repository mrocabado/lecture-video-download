package com.barbri.util;

public class AppSettings {
    public static String get(String setting) {
        return System.getenv(setting);
    }

}
