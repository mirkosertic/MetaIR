package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class DiamondShapeLoopTest {

    public static int memoryDiamond(final boolean check, final int a, final int b) {
        final int[] x = new int[10];
        for (int i = 0; i < 10; i++) {
            x[a] = 10;
            if (check) {
                x[b] = (a + b);
            } else {
                x[b] = (a - b);
            }
        }
        return x[b];
    }
}
