package de.mirkosertic.metair.ir;

public class New extends Value {

    New(final RuntimeclassReference runtimeclassReference) {
        super(runtimeclassReference.type);

        use(runtimeclassReference, new ArgumentUse(0));
    }

    @Override
    public String debugDescription() {
        return "New";
    }
}
