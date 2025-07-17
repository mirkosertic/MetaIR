package de.mirkosertic.metair;

public class Debug2 {

    static int i;

    int j;

    public static int compute(final int x, final int y) {
        int k = 0;
        for (int i = x; i < y; i++) {
            k = k + i;
        }
        return k;
    }

    public int getStatic() {
        int j = i;
        return j;
    }

    public static int getInstance() {
        Debug2 d = new Debug2();
        return d.j;
    }

    public static int invokeStatic() {
        return Target.compute(10, 20);
    }

    public double intdoubletest(int a, double b, double c, int d, int e) {
        return b + c;
    }
}
