package de.mirkosertic.metair.ir;

import java.lang.constant.ClassDesc;
import java.util.List;

public class NewMultiArray extends Value {

    NewMultiArray(final ClassDesc type, final List<Value> sizes) {
        super(type);
        for (int i = 0; i < sizes.size(); i++) {
            use(sizes.get(i), new ArgumentUse(i));
        }
    }

    @Override
    public String debugDescription() {
        return "NewMultiArray : " + DebugUtils.toString(type);
    }
}
