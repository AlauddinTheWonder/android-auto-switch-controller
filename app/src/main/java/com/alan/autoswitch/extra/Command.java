package com.alan.autoswitch.extra;

public class Command {

    // Device Commands
    public static final int NO_COMMAND_VALUE = 0;
    public static final int GET_ALL_DATA = 255;
    public static final int GET_TIME = 254;
    public static final int SET_TIME = 253;
    public static final int GET_SWITCH_NUM = 252;
    public static final int GET_MAX_SETTINGS = 251;
    public static final int GET_SWITCH_VALUE = 250;

    public static int COMMAND = NO_COMMAND_VALUE;
    public static long PARAM = NO_COMMAND_VALUE;

    public static void set(int command, long param) {
        COMMAND = command;
        PARAM = param;
    }

    public static void reset() {
        COMMAND = NO_COMMAND_VALUE;
        PARAM = NO_COMMAND_VALUE;
    }
}
