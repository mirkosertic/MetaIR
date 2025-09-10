package de.mirkosertic.metair.ir;

public class ClassInitialization extends Value {

    public final Value arg0;

    ClassInitialization(final RuntimeclassReference value) {
        super(value.type);

        this.arg0 = use(value, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "ClassInit";
    }
}
