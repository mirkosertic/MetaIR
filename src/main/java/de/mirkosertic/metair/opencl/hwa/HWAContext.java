package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.opencl.OpenCL;
import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.DeviceProperties;
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.PlatformProperties;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashMap;
import java.util.Map;

public class HWAContext implements Context {

    private final OpenCL openCl;
    private final DeviceProperties deviceProperties;
    private final Map<Kernel, HWACompiledKernel> compiledKernels;
    private final Arena arena;
    private final MemorySegment context;
    private final MemorySegment commandQueue;

    public HWAContext(final OpenCL openCl, final PlatformProperties platformProperties, final DeviceProperties deviceProperties) {
        this.openCl = openCl;
        this.deviceProperties = deviceProperties;
        this.compiledKernels = new HashMap<>();
        this.arena = Arena.ofConfined();

        final MemorySegment properties = arena.allocate(ValueLayout.JAVA_LONG, 3 * 8L);
        properties.set(ValueLayout.JAVA_LONG, 0, OpenCL.CL_CONTEXT_PLATFORM);
        properties.set(ValueLayout.JAVA_LONG, 8, platformProperties.getId());
        properties.set(ValueLayout.JAVA_LONG, 16, 0);

        final MemorySegment deviceId = arena.allocateFrom(ValueLayout.JAVA_LONG, deviceProperties.getId());
        final MemorySegment errorRet = arena.allocate(ValueLayout.JAVA_INT);

        context = openCl.clCreateContext(properties, 1, deviceId, MemorySegment.NULL, MemorySegment.NULL, errorRet);
        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
            throw new RuntimeException("clCreateContext failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
        }

        commandQueue = openCl.clCreateCommandQueue(context, deviceProperties.getId(), MemorySegment.NULL, errorRet);
        if (errorRet.get(ValueLayout.JAVA_INT, 0) != OpenCL.CL_SUCCESS) {
            throw new RuntimeException("clCreateContext failed: " + errorRet.get(ValueLayout.JAVA_INT, 0));
        }
    }

    @Override
    public void compute(final int numberOfStreams, final Kernel kernel) {
        final HWACompiledKernel compiledKernel = compiledKernels.computeIfAbsent(kernel, key -> new HWACompiledKernel(openCl, deviceProperties, key, arena, context, commandQueue));
        compiledKernel.compute(numberOfStreams);
    }

    @Override
    public void close() {
        for (final HWACompiledKernel c : compiledKernels.values()) {
            c.close();
        }
        openCl.clReleaseCommandQueue(commandQueue);
        openCl.clReleaseContext(context);
        arena.close();
    }
}
