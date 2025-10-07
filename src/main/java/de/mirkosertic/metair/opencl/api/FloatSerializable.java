package de.mirkosertic.metair.opencl.api;

import java.nio.FloatBuffer;

public interface FloatSerializable {

    void writeTo(FloatBuffer aBuffer);

    void readFrom(FloatBuffer aBuffer);
}
