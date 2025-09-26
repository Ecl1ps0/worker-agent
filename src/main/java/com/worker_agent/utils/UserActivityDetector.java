package com.worker_agent.utils;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.DWORD;

public class UserActivityDetector {

    // Windows APIs
    public interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        int GetTickCount();
    }

    public interface User32 extends Library {
        User32 INSTANCE = Native.load("user32", User32.class);

        class LASTINPUTINFO extends Structure {
            public DWORD cbSize = new DWORD(size());
            public DWORD dwTime;

            @Override
            protected java.util.List<String> getFieldOrder() {
                return java.util.List.of("cbSize", "dwTime");
            }
        }

        boolean GetLastInputInfo(LASTINPUTINFO result);
    }

    /**
     * Check if user has been active in the last N milliseconds.
     * Works only on Windows; returns a fallback on Linux/macOS.
     */
    public static boolean isUserActiveRecently(long idleThresholdMs) {
        if (Platform.isWindows()) {
            User32.LASTINPUTINFO info = new User32.LASTINPUTINFO();
            if (User32.INSTANCE.GetLastInputInfo(info)) {
                int idleTime = Kernel32.INSTANCE.GetTickCount() - info.dwTime.intValue();
                return idleTime < idleThresholdMs;
            } else {
                return true; // fallback if call fails
            }
        } else {
            // On Linux/macOS inside Docker, user activity detection usually makes no sense
            // You can either always return true, or integrate with X11 APIs if needed
            return true;
        }
    }
}
