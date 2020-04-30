package com.alan.autoswitch.extra;

public class Command {
    public static int COMMAND = Constants.NO_COMMAND_VALUE;
    public static long PARAM = Constants.NO_COMMAND_VALUE;

    public static void set(int command, long param) {
        COMMAND = command;
        PARAM = param;
    }

    public static void reset() {
        COMMAND = Constants.NO_COMMAND_VALUE;
        PARAM = Constants.NO_COMMAND_VALUE;
    }
}
