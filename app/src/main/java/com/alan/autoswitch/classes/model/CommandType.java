package com.alan.autoswitch.classes.model;

public class CommandType {
    private int command;
    private long param;

    public CommandType(int command, long param) {
        this.command = command;
        this.param = param;
    }

    public int getCommand() {
        return command;
    }

    public long getParam() {
        return param;
    }
}
