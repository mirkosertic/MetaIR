package de.mirkosertic.metair.opencl.api;

public interface DeviceProperties {

    long getId();

    String getName();

    int getNumberOfComputeUnits();

    long[] getMaxWorkItemSizes();

    long getMaxWorkGroupSize();

    long getClockFrequency();
}
