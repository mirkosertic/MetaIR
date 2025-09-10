package de.mirkosertic.metair.ir;

public class New extends Value {

    public final Value arg0;

    New(final Value runtimeclassReference) {
        super(runtimeclassReference.type);

        this.arg0 = use(runtimeclassReference, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "New";
    }
}
