package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class TypeTest {

    public Object downCastReturnObject() {
        return "hello";
    }

    public Object downCastReturnArray() {
        return new Object[10];
    }
}
