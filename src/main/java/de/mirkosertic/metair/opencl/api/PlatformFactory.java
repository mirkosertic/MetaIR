package de.mirkosertic.metair.opencl.api;

import java.util.ServiceLoader;

public abstract class PlatformFactory {

    public static PlatformFactory resolve() {
        final ServiceLoader<PlatformFactory> theLoader = ServiceLoader.load(PlatformFactory.class);
        return theLoader.iterator().next();
    }

    public Platform createPlatform() {
        return createPlatform(OpenCLOptions.defaults());
    }
    
    public abstract Platform createPlatform(OpenCLOptions aOptions);
    
    
}
