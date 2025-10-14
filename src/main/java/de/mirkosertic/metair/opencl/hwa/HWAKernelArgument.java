package de.mirkosertic.metair.opencl.hwa;

import de.mirkosertic.metair.ir.IRType;

import java.lang.reflect.Field;

public record HWAKernelArgument(String name, IRType type, Field field) {
}