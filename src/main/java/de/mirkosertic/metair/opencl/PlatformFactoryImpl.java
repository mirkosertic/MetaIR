package de.mirkosertic.metair.opencl;

import de.mirkosertic.metair.opencl.api.OpenCLOptions;
import de.mirkosertic.metair.opencl.api.Platform;
import de.mirkosertic.metair.opencl.api.PlatformFactory;
import de.mirkosertic.metair.opencl.cpu.CPUPlatform;
import de.mirkosertic.metair.opencl.hwa.HWAPlatform;

public class PlatformFactoryImpl extends PlatformFactory {

    @Override
    public Platform createPlatform(final OpenCLOptions aOptions) {
        try {
            // Try hardware acceleration first
            final OpenCL openCL = new OpenCL();
            // We wont get to this point in case system does not support OpenCL
            return new HWAPlatform(openCL, aOptions);
        } catch (final Exception e) {
            // Default to CPU
            return new CPUPlatform(aOptions);
        }
    }
}
