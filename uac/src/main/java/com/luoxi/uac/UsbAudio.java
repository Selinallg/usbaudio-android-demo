package com.luoxi.uac;

public class UsbAudio {
    static {
        System.loadLibrary("usbaudio");
    }

    public native boolean setup();
    public native void close();
    public native void loop();
    public native boolean stop();
    public native int measure();
}
