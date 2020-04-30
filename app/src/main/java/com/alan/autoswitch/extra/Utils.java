package com.alan.autoswitch.extra;

import java.util.TimeZone;

public class Utils {

    public static int getIndexByDelim(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == Constants.DEFAULT_DELIMITER) {
                return i;
            }
        }
        return -1;
    }

    // Get system's current time in UTC
    public static long getCurrentTimeUTC() {
        long timestamp = System.currentTimeMillis() / 1000;
        long offset = TimeZone.getDefault().getRawOffset() / 1000;
        return timestamp + offset;
    }
}
