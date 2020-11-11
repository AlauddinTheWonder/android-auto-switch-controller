package com.alan.autoswitch.classes.model;

public class SwitchModel {
    private int pin;
    private int on;
    private int off;
    private int index;

    public SwitchModel(int pin, int on, int off, int index){
        this.pin = pin;
        this.on = on;
        this.off = off;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public int getOn() {
        return on;
    }

    public void setOn(int on) {
        this.on = on;
    }

    public int getOff() {
        return off;
    }

    public void setOff(int off) {
        this.off = off;
    }
}
