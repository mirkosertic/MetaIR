package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class DiamondShapeTest {

    public static int memoryDiamond(final boolean check, final int a, final int b) {
        final int[] x = new int[10];
        x[a] = 10;
        if (check) {
            x[b] = 20;
        } else {
            x[b] = 30;
        }
        return x[b];
    }
}
