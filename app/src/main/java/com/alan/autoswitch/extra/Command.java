package com.alan.autoswitch.extra;

public class Command {
    public static int COMMAND = 0;
    public static long PARAM = 0;

    public static void set(int command, long param) {
        COMMAND = command;
        PARAM = param;
    }
}
