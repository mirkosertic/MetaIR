package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class SynchronizedTest {

    private int compute(int a) {
        return a + 1;
    }

    public void synchronizedTest() {
        synchronized (this) {
            int x = compute(10);
        }
    }
}
