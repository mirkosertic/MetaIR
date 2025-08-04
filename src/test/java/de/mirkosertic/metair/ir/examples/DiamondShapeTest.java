package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class DiamondShapeTest {

    public static int memoryDiamond(boolean check, int a, int b) {
        int[] x = new int[10];
        x[a] = 10;
        int result;
        if (check) {
            x[b] = 20;
        } else {
            x[b] = 30;
        }
        return x[b];
    }
}
