package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.opencl.OpenCL;
import de.mirkosertic.metair.opencl.api.Context;
import de.mirkosertic.metair.opencl.api.Kernel;
import de.mirkosertic.metair.opencl.api.OpenCLOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static de.mirkosertic.metair.opencl.api.GlobalFunctions.set_global_id;
import static de.mirkosertic.metair.opencl.api.GlobalFunctions.set_global_size;

public class HWAContext implements Context {

    private final OpenCL openCl;
    private final OpenCLOptions openCLOptions;
    private final Map<Kernel, HWACompiledKernel> compiledKernels;

    public HWAContext(final OpenCL openCl, final OpenCLOptions openCLOptions) {
        this.openCl = openCl;
        this.openCLOptions = openCLOptions;
        this.compiledKernels = new HashMap<>();
    }

    @Override
    public void compute(final int numberOfStreams, final Kernel kernel) {

        final HWACompiledKernel c = compiledKernels.computeIfAbsent(kernel, HWACompiledKernel::new);

        IntStream.range(0, numberOfStreams)
                .parallel()
                .forEach(workItemId->{
                    try {
                        set_global_size(0, numberOfStreams);
                        set_global_id(0, workItemId);
                        kernel.processWorkItem();
                    } catch (final Exception e) {
                        throw new IllegalStateException("Kernel execution (single work item) failed.", e);
                    }
                }); // blocks until all work-items are complete
    }

    @Override
    public void close() {
        for (final HWACompiledKernel c : compiledKernels.values()) {
            c.close();
        }
    }
}
