package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.ir.IRType;

public class HWAKernelArgument {
    private final String name;
    private final IRType type;

    public HWAKernelArgument(final String name, final IRType type) {
        this.name = name;
        this.type = type;
    }
}