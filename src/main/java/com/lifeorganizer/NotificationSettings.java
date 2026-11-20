package com.lifeorganizer;

import java.util.prefs.Preferences;

/** Per-module notification preferences, stored locally for the signed-in account. */
public final class NotificationSettings {
    private NotificationSettings() {}
    private static Preferences prefs(int userId) { return Preferences.userRoot().node("LifeOrganizer/users/" + userId + "/notifications"); }
    public static boolean popup(int userId, String module) { return prefs(userId).getBoolean(module + ".popup", true); }
    public static boolean sound(int userId, String module) { return prefs(userId).getBoolean(module + ".sound", true); }
    public static void set(int userId, String module, boolean popup, boolean sound) {
        Preferences p = prefs(userId); p.putBoolean(module + ".popup", popup); p.putBoolean(module + ".sound", sound);
    }
}
