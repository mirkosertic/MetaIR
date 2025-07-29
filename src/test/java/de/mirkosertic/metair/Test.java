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
        return a + 1;
    }

    public int staticInvocationInMember() {
        return compute(10);
    }

    public static Test newInstance() {
        return new Test();
    }
}
