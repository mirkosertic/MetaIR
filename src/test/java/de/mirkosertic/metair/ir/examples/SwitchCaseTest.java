package de.mirkosertic.metair.ir.examples;

import de.mirkosertic.metair.ir.test.MetaIRTest;

@MetaIRTest
public class SwitchCaseTest {

    int simpleSwitch(final int value) {
        return switch (value) {
            case 10 -> 100;
            case 20 -> 200;
            default -> 300;
        };
    }
}
