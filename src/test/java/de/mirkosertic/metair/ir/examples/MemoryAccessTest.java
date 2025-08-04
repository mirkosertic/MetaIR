package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class MemoryAccessTest {

    int instancmember;
    static int staticmember;

    public void memoryFlow() {
        int a = instancmember;
        int b = staticmember;

        staticmember = a;
        instancmember = b;
    }
}
