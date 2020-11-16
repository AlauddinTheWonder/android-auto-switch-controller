package com.alan.autoswitch.classes.model;

import java.util.ArrayList;
import java.util.List;

public class CommandList {
    private static List<CommandType> list = new ArrayList<CommandType>();

    public static void add(int command, long param) {
        list.add(new CommandType(command, param));
    }

    public static CommandType get(int index) {
        return list.get(index);
    }

    public static void set(int index, CommandType command) {
        list.set(index, command);
    }

    public static void remove(int index) {
        list.remove(index);
    }

    public static int size() {
        return list.size();
    }

    public static void reset() {
        list.clear();
    }
}
