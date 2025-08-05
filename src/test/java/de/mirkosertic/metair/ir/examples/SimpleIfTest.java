package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class SimpleIfTest {

    public static int simpleIf(int a, int b, int c, int d) {
        if (a > b) {
            return c;
        } else {
            return d;
        }
    }

    public static int simpleIfJoin(int a, int b, int c, int d) {
        int r;
        if (a > b) {
            r = c;
        } else {
            r = d;
        }
        return r;
    }
}
