package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class LoopInvariantTest {

    public static int forLoop(int a, int b, int c) {
        int j = 10;
        for (int i = 0; i < a; i++) {
            j += i + b + c;
        }
        return a;
    }
}
