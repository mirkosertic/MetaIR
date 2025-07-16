package de.mirkosertic.metair.ir;

import java.util.Objects;

public class ArgumentUse extends DataFlowUse {

    protected final int index;

    ArgumentUse(final int index) {
        this.index = index;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final ArgumentUse that = (ArgumentUse) o;
        return index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(index);
    }
}
