package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class SwitchCaseTest {

    int simpleSwitch(final int value) {
        switch (value) {
            case 10:
                return 100;
            case 20:
                return 200;
            default:
                return 300;
        }
    }
}
