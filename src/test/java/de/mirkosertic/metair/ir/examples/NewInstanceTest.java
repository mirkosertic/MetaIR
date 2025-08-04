package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest(includeConstructors = true)
public class NewInstanceTest {

    public NewInstanceTest() {
    }

    public void newInstance() {
        new NewInstanceTest();
    }
}
