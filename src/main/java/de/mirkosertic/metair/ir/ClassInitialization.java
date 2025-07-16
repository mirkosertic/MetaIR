package de.mirkosertic.metair.ir;

public class ClassInitialization extends Node {

    ClassInitialization(final RuntimeclassReference value) {
        use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ClassInit";
    }
}
