package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class StaticMethodInvokeTest {

    private static int compute(final int a) {
        return a + 1;
    }

    public int staticInvocationInMember() {
        return compute(10);
    }
}
