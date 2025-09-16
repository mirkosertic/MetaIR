package de.mirkosertic.metair.ir;

public class ClassInitialization extends Value {

    ClassInitialization(final RuntimeclassReference value) {
        super(value.type);

        use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ClassInit";
    }
}
