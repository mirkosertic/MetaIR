package de.mirkosertic.metair;

public class Test {

    public Test() {
    }

    public static int simpleIf(final int a, final int b, final int c, final int d) {
        if (a > b) {
            return c;
        } else {
            return d;
        }
    }

    public static int forLoop(final int a) {
        for (int i = 0; i < a; i++) {
            compute(i);
        }
        return a;
    }

    private static int compute(final int a) {
        return a + 1 + compute(a + 1);
    }

    public int staticInvocationInMember() {
        return compute(10);
    }

    public static Test newInstance() {
        return new Test();
    }

    public void synchronizedTest() {
        synchronized (this) {
            int x = compute(10);
        }
    }

    public void throwTest() {
        throw new RuntimeException("Test");
    }

    public char getChar() {
        return 'a';
    }

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

    int instancmember;
    static int staticmember;

    public void memoryFlow() {
        int a = instancmember;
        int b = staticmember;

        staticmember = a;
        instancmember = b;
    }

}
