package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class ArrayTest {

    public void newByteArray()  {
        final byte[] c = new byte[10];
        c[0] = 10;
        final byte c2 = c[1];
        final int l = c.length;
    }

    public void newCharArray()  {
        final char[] c = new char[10];
        c[0] = 10;
        final char c2 = c[1];
        final int l = c.length;
    }

    public void newShortArray()  {
        final short[] c = new short[10];
        c[0] = 10;
        final short c2 = c[1];
        final int l = c.length;
    }

    public void newIntArray()  {
        final int[] c = new int[10];
        c[0] = 10;
        final int c2 = c[1];
        final int l = c.length;
    }

    public void newLongArray()  {
        final long[] c = new long[10];
        c[0] = 10L;
        final long c2 = c[1];
        final int l = c.length;
    }

    public void newFloatArray()  {
        final float[] c = new float[10];
        c[0] = 10f;
        final float c2 = c[1];
        final int l = c.length;
    }

    public void newDoubleArray()  {
        final double[] c = new double[10];
        c[0] = 10d;
        final double c2 = c[1];
        final int l = c.length;
    }

    public void newBooleanArray()  {
        final boolean[] c = new boolean[10];
        c[0] = true;
        final boolean c2 = c[1];
        final int l = c.length;
    }

    public void newObjectArray()  {
        final Object[] c = new Object[10];
        c[0] = this;
        final Object c2 = c[1];
        final int l = c.length;
    }

    public void newMultiObjectArray()  {
        final Object[] c = new Object[10][20];
        c[0] = new Object[5];
        final Object c2 = c[1];
        final int l = c.length;
    }
}
