package de.mirkosertic.metair.opencl.api;

public interface Platform {

    PlatformProperties getPlatformProperties();

    DeviceProperties getDeviceProperties();

    Context createContext();
}
