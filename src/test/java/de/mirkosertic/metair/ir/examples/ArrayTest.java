package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class ArrayTest {

    public void newByteArray()  {
        byte[] c = new byte[10];
        c[0] = 10;
        byte c2 = c[1];
        int l = c.length;
    }

    public void newCharArray()  {
        char[] c = new char[10];
        c[0] = 10;
        char c2 = c[1];
        int l = c.length;
    }

    public void newShortArray()  {
        short[] c = new short[10];
        c[0] = 10;
        short c2 = c[1];
        int l = c.length;
    }

    public void newIntArray()  {
        int[] c = new int[10];
        c[0] = 10;
        int c2 = c[1];
        int l = c.length;
    }

    public void newLongArray()  {
        long[] c = new long[10];
        c[0] = 10L;
        long c2 = c[1];
        int l = c.length;
    }

    public void newFloatArray()  {
        float[] c = new float[10];
        c[0] = 10f;
        float c2 = c[1];
        int l = c.length;
    }

    public void newDoubleArray()  {
        double[] c = new double[10];
        c[0] = 10d;
        double c2 = c[1];
        int l = c.length;
    }
}
