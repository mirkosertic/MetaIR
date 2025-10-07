package de.mirkosertic.metair.opencl.api;

import java.nio.FloatBuffer;

@OpenCLType(name = "float2", elementCount = 2)
public class Float2 implements FloatSerializable {

    public float s0;
    public float s1;

    @OpenCLFunction(value = "float2", literal = true)
    public static Float2 float2(final float aS0, final float aS1) {
        return new Float2(aS0, aS1);
    }

    private Float2(final float aS0, final float aS1) {
        s0 = aS0;
        s1 = aS1;
    }

    @Override
    public void writeTo(final FloatBuffer aBuffer) {
        aBuffer.put(s0).put(s1);
    }

    @Override
    public void readFrom(final FloatBuffer aBuffer) {
        s0 = aBuffer.get();
        s1 = aBuffer.get();
    }

    @Override
    public String toString() {
        return "float2{" +
                "s0=" + s0 +
                ", s1=" + s1 +
                '}';
    }

    static Float2 normalize(final Float2 aVector) {
        final float length = aVector.length();
        return float2(aVector.s0 / length, aVector.s1 / length);
    }

    float length() {
        float theSquareSum = 0.0f;
        theSquareSum += s0 * s0;
        theSquareSum += s1 * s1;
        return (float) Math.sqrt(theSquareSum);

    }

    Float2 cross(final Float2 aOtherVector) {
        return new Float2(
            s0 * aOtherVector.s0,
            s1 * aOtherVector.s1
        );
    }

    float dot(final Float2 aOtherVector) {
        float theDotProduct = 0.0f;
        theDotProduct += s0 * aOtherVector.s0;
        theDotProduct += s1 * aOtherVector.s1;
        return theDotProduct;
    }
}