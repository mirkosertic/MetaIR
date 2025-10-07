package de.mirkosertic.metair.opencl.api;

public interface Context extends AutoCloseable {

    void compute(int numberOfStreams, Kernel kernel);
}
