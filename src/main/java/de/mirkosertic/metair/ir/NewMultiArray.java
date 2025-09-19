package de.mirkosertic.metair.ir;

import java.util.List;

public class NewMultiArray extends Value {

    NewMultiArray(final IRType.MetaClass type, final List<Value> dimensions) {
        super(type);

        for (int i = 0; i < dimensions.size(); i++) {
            final Value v = dimensions.get(i);
            if (!v.type.equals(IRType.CD_int)) {
                illegalArgument("Array dimension must be int, but was " + TypeUtils.toString(v.type) + " for dimension " + (i + 1));
            }
            use(v, new ArgumentUse(i));
        }
    }

    @Override
    public String debugDescription() {
        return "NewMultiArray : " + TypeUtils.toString(type);
    }
}
