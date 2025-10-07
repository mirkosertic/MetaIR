package de.mirkosertic.metair.opencl.api;

public class VectorFunctions {

    @OpenCLFunction("normalize")
    public static Float2 normalize(final Float2 aVector) {
        return Float2.normalize(aVector);
    }

    @OpenCLFunction("normalize")
    public static Float4 normalize(final Float4 aVector) {
        return Float4.normalize(aVector);
    }

    @OpenCLFunction("length")
    public static float length(final Float2 aVector) {
        return aVector.length();
    }

    @OpenCLFunction("length")
    public static float length(final Float4 aVector) {
        return aVector.length();
    }

    @OpenCLFunction("cross")
    public static Float2 cross(final Float2 aVector, final Float2 aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("cross")
    public static Float4 cross(final Float4 aVector, final Float4 aOtherVector) {
        return aVector.cross(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(final Float2 aVector, final Float2 aOtherVector) {
        return aVector.dot(aOtherVector);
    }

    @OpenCLFunction("dot")
    public static float dot(final Float4 aVector, final Float4 aOtherVector) {
        return aVector.dot(aOtherVector);
    }
}
