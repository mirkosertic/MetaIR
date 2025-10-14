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
            // We won't get to this point in case the system does not support OpenCL
            return new HWAPlatform(openCL, aOptions);
        } catch (final Exception e) {
            // Default to CPU emulation
            return new CPUPlatform(aOptions);
        }
    }
}
