package de.mirkosertic.metair;

public class Debug3 {

    public Debug3() {
    }

    public static int simpleIf(int a, int b, int c, int d) {
        if (a > b) {
            return c;
        } else {
            return d;
        }
    }

    public static int forLoop(int a) {
        int k = 0;
        for (int i = 0; i < a; i++) {
            k = k + i;
        }
        return k;
    }

    private static int compute(int a) {
        return a + 1;
    }

    public int staticInvocationInMember() {
        return compute(10);
    }
}
