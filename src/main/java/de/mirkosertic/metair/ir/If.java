package de.mirkosertic.metair.ir;

public class If extends Node {

    If(final Value condition) {
        use(condition, new ArgumentUse(0));
    }
}
